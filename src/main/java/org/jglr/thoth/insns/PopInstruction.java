package org.jglr.thoth.insns;

import org.jglr.thoth.InterpreterState;
import org.jglr.thoth.ThothInterpreter;
import org.jglr.thoth.ThothValue;

public class PopInstruction extends ThothCommandment {
    public PopInstruction() {
        super(Type.TEXT);
    }

    @Override
    public String execute(ThothValue[] params, InterpreterState state, ThothInterpreter interpreter) {
        ThothValue value = state.pop();
        if(value.getType() == ThothValue.Types.BOOL) {
            return (boolean)value.getValue() ? "1" : "0";
        }
        return value.getValue().toString();
    }

    @Override
    public String toString() {
        return "POP";
    }
}
