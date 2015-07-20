package org.jglr.thoth.insns;

import org.jglr.thoth.interpreter.InterpreterState;
import org.jglr.thoth.interpreter.ThothInterpreter;
import org.jglr.thoth.ThothValue;

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
