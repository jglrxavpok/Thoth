package thoth.insns;

import thoth.interpreter.InterpreterState;
import thoth.interpreter.ThothInterpreter;
import thoth.lang.ThothValue;

public class LoadConstantInsn extends ThothInstruction {
    private final String val;

    public LoadConstantInsn(String s) {
        super(Type.VARIABLE);
        this.val = s;
    }

    @Override
    public String execute(ThothValue[] params, InterpreterState state, ThothInterpreter interpreter) {
        state.push(new ThothValue(ThothValue.Types.TEXT, val));
        return "";
    }

    @Override
    public String toString() {
        return "LDC "+val;
    }
}
