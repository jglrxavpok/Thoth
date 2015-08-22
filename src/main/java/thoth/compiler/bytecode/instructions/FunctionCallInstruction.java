package thoth.compiler.bytecode.instructions;

import thoth.compiler.resolver.ResolvedClass;

public class FunctionCallInstruction extends ThothInstruction {

    private final ResolvedClass owner;
    private final String name;
    private final int argumentCount;

    public FunctionCallInstruction(ResolvedClass owner, String name, int argumentCount) {
        super();
        this.owner = owner;
        this.name = name;
        this.argumentCount = argumentCount;
    }

    public ResolvedClass getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public int getArgumentCount() {
        return argumentCount;
    }

    public String toString() {
        return "func_call "+owner.getName()+" "+name;
    }
}
