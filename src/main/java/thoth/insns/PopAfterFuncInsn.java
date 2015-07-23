package thoth.insns;

import thoth.interpreter.InterpreterState;
import thoth.interpreter.ThothInterpreter;
import thoth.lang.ThothValue;

public class PopAfterFuncInsn extends ThothInstruction {
    public PopAfterFuncInsn() {
        super(Type.CONDITION);
    }

    @Override
    public String execute(ThothValue[] params, InterpreterState state, ThothInterpreter interpreter) {
        return "";
    }

    @Override
    public String toString() {
        return "POP AfterFunc";
    }
}
