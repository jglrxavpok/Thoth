package thoth.compiler.bytecode.instructions;

public class LoadArgumentInstruction extends ThothInstruction {

    private final int index;

    public LoadArgumentInstruction(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public String toString() {
        return "load_arg "+index;
    }
}
