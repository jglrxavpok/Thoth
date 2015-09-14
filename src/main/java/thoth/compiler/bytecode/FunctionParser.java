package thoth.compiler.bytecode;

import thoth.compiler.ThothCompileError;
import thoth.compiler.bytecode.instructions.*;
import thoth.compiler.resolver.ResolvedClass;
import thoth.compiler.resolver.ResolvedFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class FunctionParser {

    public List<CompiledFunction> parse(ResolvedFunction function) {
        CompiledFunction result = new CompiledFunction(function.getName(), function.getArgumentNames(), function.getTypes(), new ArrayList<>(), new ArrayList<>());
        String code = function.getCode();
        parseBlock(function, result, 0, code.length());
        List<CompiledFunction> list = new ArrayList<>();
        return getAllFunctions(result, list);
    }

    private List<CompiledFunction> getAllFunctions(CompiledFunction result, List<CompiledFunction> funcs) {
        funcs.add(result);
        result.getSubfunctions().forEach(f -> getAllFunctions(f, funcs));
        return funcs;
    }

    private int parseBlock(ResolvedFunction function, CompiledFunction destination, int start, int length) {
        System.out.println("compiling " + destination.getName()+" with code: \n"+function.getCode().substring(start, length+start));
        List<ThothInstruction> instructions = destination.getInstructions();
        List<SubFunction> subfunctions = destination.getSubfunctions();
        boolean inString = false;
        Stack<String> functionCalls = new Stack<>();
        String code = function.getCode();
        char[] chars = code.toCharArray();
        StringBuilder buffer = new StringBuilder();
        for(int index = start;index<length+start;index++) {
            char read = chars[index];
            if(read == '"') {
                inString = !inString;
                if(!inString) { // No longer in a string
                    String text = buffer.toString();
                    instructions.add(new LoadTextInstruction(text));
                    empty(buffer);
                } else { // Starting a string, check for arguments and/or constants
                    String arg = buffer.toString();
                    handleArgsAndConstants(arg, function, instructions);
                    empty(buffer);
                }
            } else if(read == '(' && !inString) {
                String functionName = buffer.toString();
                empty(buffer);
                functionCalls.push(functionName);
            } else if(read == ')' && !inString) {
                String functionName = functionCalls.pop();
                String arg = buffer.toString();
                handleArgsAndConstants(arg, function, instructions);
                if(!handleBranching(functionName, function, instructions, index, chars)) {
                    handleFunction(functionName, function, instructions);
                }
                empty(buffer);
            } else if(read == ' ' && !inString) {
                String arg = buffer.toString();
                handleArgsAndConstants(arg, function, instructions);
                empty(buffer);
            } else if(read == '_' && !inString) {
                String arg = buffer.toString();
                handleArgsAndConstants(arg, function, instructions);
                empty(buffer);
                instructions.add(new LoadSpaceInstruction());
            } else if(read == '{' && !inString) {
                // Start inner function
                SubFunction subfunc = new SubFunction(function.getName()+"$"+subfunctions.size(), function.getArgumentNames());
                subfunctions.add(subfunc);
                index += parseBlock(function, subfunc, index+1, findRelativeBracket(index, length, chars));

                ThothInstruction last = instructions.get(instructions.size()-1);
                if(last instanceof ConditionalCallInstruction) {
                    ConditionalCallInstruction ins = (ConditionalCallInstruction) last;
                    ins.setCall(new FunctionCallInstruction(function.getOwner(), subfunc.getName(), function.getArgumentCount()));
                }
            } else if(read == '}' && !inString) {
                // NOP
            } else {
                buffer.append(read);
            }
        }
        String arg = buffer.toString();
        handleArgsAndConstants(arg, function, instructions);

        return length; // TODO
    }

    private int findRelativeBracket(int index, int length, char[] chars) {
        if(chars[index] != '{') {
            throw new IllegalArgumentException("No opening curly bracket at index "+index);
        }
        boolean inString = false;
        int start = index;
        int brackets = 0;
        for(;index<length;index++) {
            char c = chars[index];
            System.out.print(c);
            if(c == '{' && !inString) {
                brackets++;
            } else if(c == '}' && !inString) {
                brackets--;
                if(brackets == 0) {
                    return index-start-1;
                }
            } else if(c == '"') {
                inString = !inString;
            }
        }
        System.out.println("not found, "+index+", "+length+", "+brackets);
        return -1;
    }

    private boolean handleBranching(String functionName, ResolvedFunction function, List<ThothInstruction> instructions, int index, char[] chars) {
        if(functionName.equals("if")) {
            instructions.add(new ConditionalCallInstruction());
            return true;
        }
        return false;
    }

    private void handleArgsAndConstants(String arg, ResolvedFunction function, List<ThothInstruction> instructions) {
        if(!arg.isEmpty()) {
            ThothInstruction insn = loadConstant(arg, function, instructions);
            if(insn != null) { // There is an instruction for this
                instructions.add(insn);
                return;
            }
            int argIndex = -1;
            for (int i = 0; i < function.getArgumentNames().length; i++) {
                String n = function.getArgumentNames()[i];
                if (n.equals(arg)) {
                    argIndex = i;
                    break;
                }
            }
            if (argIndex == -1) {
                throw new ThothCompileError("Could not find argument named " + arg, -1, -1); // TODO: proper line number
            }
            instructions.add(new LoadArgumentInstruction(argIndex));
        }
    }

    private ThothInstruction loadConstant(String arg, ResolvedFunction function, List<ThothInstruction> instructions) {
        switch (arg) {
            case "false":
            case "False":
            case "FALSE":
                return new LoadBooleanInstruction(false);

            case "true":
            case "True":
            case "TRUE":
                return new LoadBooleanInstruction(true);
        }
        return null;
    }

    private void handleFunction(String functionName, ResolvedFunction function, List<ThothInstruction> instructions) {
        if(functionName.equals("native")) {
            if(instructions.size() >= 1) {
                ThothInstruction previous = instructions.remove(instructions.size() - 1);
                if(previous instanceof LoadTextInstruction) {
                    LoadTextInstruction text = (LoadTextInstruction)previous;
                    String location = text.getText();
                    instructions.add(new NativeCallInstruction(function, location));
                } else {
                    throw new ThothCompileError("native(location) takes only one parameter, and it must be a constant. Found: "+previous, -1,-1); // TODO: proper line number
                }
            } else {
                throw new ThothCompileError("Cannot call native(location) with no arguments", -1,-1); // TODO: proper line number
            }
        } else {
            FunctionInfos infos = findInfos(function.getOwner(), functionName);
            instructions.add(new FunctionCallInstruction(infos.getOwner(), functionName, infos.getArgCount()));
        }
    }

    private FunctionInfos findInfos(ResolvedClass owner, String functionName) {
        for(ResolvedClass c : owner.getImports()) {
            for(ResolvedFunction f : c.getFunctions()) {
                if(f.getName().equals(functionName))
                    return new FunctionInfos(c, f.getArgumentNames().length);
            }
        }
        for(ResolvedFunction f : owner.getFunctions()) {
            if(f.getName().equals(functionName))
                return new FunctionInfos(owner, f.getArgumentNames().length);
        }
        throw new ThothCompileError("No method found with name "+functionName, -1, -1);
    }

    private void empty(StringBuilder buffer) {
        buffer.delete(0, buffer.length());
    }
}
