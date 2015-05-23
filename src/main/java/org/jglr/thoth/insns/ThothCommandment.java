package org.jglr.thoth.insns;

import org.jglr.thoth.Constants;
import org.jglr.thoth.InterpreterState;
import org.jglr.thoth.ThothInterpreter;
import org.jglr.thoth.ThothValue;

public abstract class ThothCommandment implements Constants {
    public enum Type {
        TEXT, CONDITION, VARIABLE, LABEL
    }

    private final Type type;

    public ThothCommandment(Type type) {
        this.type = type;
    }

    public abstract String execute(ThothValue[] params, InterpreterState state, ThothInterpreter interpreter);

    public Type getType() {
        return type;
    }

    public abstract String toString();
}
