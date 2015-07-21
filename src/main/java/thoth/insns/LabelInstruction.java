package thoth.insns;

import thoth.interpreter.InterpreterState;
import thoth.interpreter.ThothInterpreter;
import thoth.lang.ThothValue;

public class LabelInstruction extends ThothInstruction {
    private final String label;

    public LabelInstruction(String label) {
        super(Type.LABEL);
        this.label = label;
    }

    @Override
    public String execute(ThothValue[] params, InterpreterState state, ThothInterpreter interpreter) {
        state.label = label;
        return "";
    }

    @Override
    public String toString() {
        return "LABEL "+label;
    }

    public String getLabel() {
        return label;
    }
}
