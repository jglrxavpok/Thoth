package thoth.insns;

import thoth.interpreter.InterpreterState;
import thoth.interpreter.ThothInterpreter;
import thoth.lang.ThothValue;

public class PushCondition extends ThothInstruction {
    public PushCondition() {
        super(Type.VARIABLE);
    }

    @Override
    public String execute(ThothValue[] params, InterpreterState state, ThothInterpreter interpreter) {
        ThothValue val = state.pop();
        boolean condition;
        if(val.getType() == ThothValue.Types.BOOL) {
            condition = (boolean) val.getValue();
        } else if(val.getType() == ThothValue.Types.NULL) {
            condition = false;
        } else {
            condition = val.getValue() != null;
        }
        state.pushCondition(condition);
        return "";
    }

    @Override
    public String toString() {
        return "PUSH CONDITION";
    }
}
