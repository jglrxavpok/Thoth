package thoth.compiler.bytecode.instructions;

public class LoadTextInstruction extends ThothInstruction {

    private final String text;

    public LoadTextInstruction(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public String toString() {
        return "load_text \""+text+"\"";
    }
}
