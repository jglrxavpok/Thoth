package thoth.insns;

import thoth.Constants;
import thoth.interpreter.InterpreterState;
import thoth.interpreter.ThothInterpreter;
import thoth.lang.ThothValue;

public abstract class ThothInstruction implements Constants {
    public enum Type {
        TEXT, CONDITION, VARIABLE, LABEL
    }

    private final Type type;

    public ThothInstruction(Type type) {
        this.type = type;
    }

    public abstract String execute(ThothValue[] params, InterpreterState state, ThothInterpreter interpreter);

    public Type getType() {
        return type;
    }

    public abstract String toString();
}
