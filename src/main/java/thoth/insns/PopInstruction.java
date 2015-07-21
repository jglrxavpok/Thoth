package thoth.insns;

import thoth.interpreter.InterpreterState;
import thoth.interpreter.ThothInterpreter;
import thoth.lang.ThothValue;

public class PopInstruction extends ThothInstruction {
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
