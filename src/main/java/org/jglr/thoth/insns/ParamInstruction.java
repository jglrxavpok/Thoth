package org.jglr.thoth.insns;

import org.jglr.thoth.ThothInterpreter;
import org.jglr.thoth.ThothValue;
import org.jglr.thoth.InterpreterState;

public class ParamInstruction extends ThothCommandment {
    private final int index;

    public ParamInstruction(int index) {
        super(Type.VARIABLE);
        this.index = index;
    }

    @Override
    public String execute(ThothValue[] params, InterpreterState state, ThothInterpreter interpreter) {
        state.variable = index;
        state.push(params[index]);
        return "";
    }

    @Override
    public String toString() {
        return "VAR "+index;
    }
}
