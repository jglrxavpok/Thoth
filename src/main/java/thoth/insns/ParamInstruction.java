package thoth.insns;

import thoth.interpreter.ThothInterpreter;
import thoth.lang.ThothValue;
import thoth.interpreter.InterpreterState;

public class ParamInstruction extends ThothInstruction {
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

    public int getIndex() {
        return index;
    }

    @Override
    public String toString() {
        return "PARAM "+index;
    }
}
