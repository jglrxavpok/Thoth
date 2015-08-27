package thoth.compiler.bytecode.instructions;

public class LoadBooleanInstruction extends ThothInstruction {
    private final boolean value;

    public LoadBooleanInstruction(boolean value) {
        super();
        this.value = value;
    }

    public boolean getValue() {
        return value;
    }
}
