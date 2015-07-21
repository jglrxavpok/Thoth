package thoth.insns;

import thoth.interpreter.InterpreterState;
import thoth.interpreter.ThothInterpreter;
import thoth.lang.ThothValue;

public class TextInstruction extends ThothInstruction {
    private final String value;

    public TextInstruction(String s) {
        super(Type.TEXT);
        this.value = s;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String execute(ThothValue[] params, InterpreterState state, ThothInterpreter interpreter) {
        return value;
    }

    @Override
    public String toString() {
        return "TEXT \""+value+"\"";
    }
}
