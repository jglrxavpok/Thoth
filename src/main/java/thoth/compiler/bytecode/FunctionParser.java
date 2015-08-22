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
            } else if(read == ' ' && !inString) {
                String arg = buffer.toString();
                if(!arg.isEmpty()) {
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
                empty(buffer);
            } else {
                buffer.append(read);
            }
            // TODO
        }

        String arg = buffer.toString();
        if(!arg.isEmpty()) {
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
        return result;
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
        return null;
    }

    private void empty(StringBuilder buffer) {
        buffer.delete(0, buffer.length());
    }
}
