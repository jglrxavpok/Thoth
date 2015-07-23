package thoth.insns;

import thoth.interpreter.InterpreterState;
import thoth.interpreter.ThothInterpreter;
import thoth.lang.ThothValue;

public class LoadLocalInsn extends ThothInstruction {
    private final int varIndex;

    public LoadLocalInsn(int varIndex) {
        super(Type.VARIABLE);
        this.varIndex = varIndex;
    }

    @Override
    public String execute(ThothValue[] params, InterpreterState state, ThothInterpreter interpreter) {
        state.push(params[varIndex]);
        return "";
    }

    @Override
    public String toString() {
        return "LocLdc "+varIndex;
    }
}
