package thoth.compiler.bytecode.instructions;

/**
 * Pseudo instruction used as a bridge between the code represented as text and bytecode.
 */
public abstract class ThothInstruction {

    /**
     * Generates a human readable String representation of this object
     * @return
     *        A String representation of this object
     */
    public abstract String toString();
}