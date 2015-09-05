package thoth.compiler.bytecode.instructions;

/**
 * Instruction that represents the loading of an argument onto the stack. Only the argument index is provided.
 */
public class LoadArgumentInstruction extends ThothInstruction {

    /**
     * The argument index
     */
    private final int index;

    /**
     *Creates a new LoadArgumentInstruction instance
     * @param index
     *             The index of the argument
     */
    public LoadArgumentInstruction(int index) {
        this.index = index;
    }

    /**
     * Gets the argument index
     * @return
     *         The argument index
     */
    public int getIndex() {
        return index;
    }

    /**
     * Generates a human readable String representation of this object
     * @return
     *        A String representation of this object
     */
    public String toString() {
        return "load_arg "+index;
    }
}
