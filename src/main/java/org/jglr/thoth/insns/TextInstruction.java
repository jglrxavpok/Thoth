package org.jglr.thoth.insns;

import org.jglr.thoth.interpreter.InterpreterState;
import org.jglr.thoth.interpreter.ThothInterpreter;
import org.jglr.thoth.ThothValue;

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
