package thoth.compiler.bytecode;

import thoth.compiler.ThothCompileError;
import thoth.compiler.bytecode.instructions.*;
import thoth.compiler.resolver.ResolvedClass;
import thoth.compiler.resolver.ResolvedFunction;

import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

public class FunctionParser {

    public CompiledFunction parse(ResolvedFunction function) {
        String code = function.getCode();
        int index = 0;
        char[] chars = code.toCharArray();
        List<ThothInstruction> instructions = new LinkedList<>();
        CompiledFunction result = new CompiledFunction(function.getName(), function.getArgumentNames(), function.getTypes(), instructions);
        StringBuilder buffer = new StringBuilder();
        boolean inString = false;
        Stack<String> functionCalls = new Stack<>();
        for(;index<chars.length;index++) {
            char read = chars[index];
            if(read == '"') {
                inString = !inString;
                if(!inString) { // No longer in a string
                    String text = buffer.toString();
                    instructions.add(new LoadTextInstruction(text));
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
                handleFunction(functionName, function, instructions);
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
            } else {
                buffer.append(read);
            }
            // TODO
        }

        String arg = buffer.toString();
        handleArgsAndConstants(arg, function, instructions);
        return result;
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
