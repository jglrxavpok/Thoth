package thoth.compiler.bytecode;

import thoth.compiler.ThothType;
import thoth.compiler.bytecode.instructions.ThothInstruction;

import java.util.List;

public class CompiledFunction {

    private final String name;
    private final String[] arguments;
    private final ThothType[] returnType;
    private final List<ThothInstruction> instructions;

    CompiledFunction(String name, String[] arguments, ThothType[] returnType, List<ThothInstruction> instructions) {
        this.name = name;
        this.arguments = arguments;
        this.returnType = returnType;
        this.instructions = instructions;
    }

    public String getName() {
        return name;
    }

    public String[] getArguments() {
        return arguments;
    }

    public ThothType[] getReturnType() {
        return returnType;
    }

    public List<ThothInstruction> getInstructions() {
        return instructions;
    }
}
