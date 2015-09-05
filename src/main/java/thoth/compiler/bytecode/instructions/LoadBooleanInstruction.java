package thoth.compiler.bytecode.instructions;

/**
 * Instruction representing the loading of a BoolValue onto the stack.
 */
public class LoadBooleanInstruction extends ThothInstruction {

    private final boolean value;

    public LoadBooleanInstruction(boolean value) {
        super();
        this.value = value;
    }

    public boolean getValue() {
        return value;
    }

    public String toString() {
        return "load_bool "+value;
    }
}
