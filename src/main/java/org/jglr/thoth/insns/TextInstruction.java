package org.jglr.thoth.insns;

import org.jglr.thoth.InterpreterState;
import org.jglr.thoth.ThothInterpreter;
import org.jglr.thoth.ThothValue;

public class TextInstruction extends ThothCommandment {
    private final String value;

    public TextInstruction(String s) {
        super(Type.TEXT);
        this.value = s;
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
