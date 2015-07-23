package thoth.insns;

import thoth.interpreter.InterpreterState;
import thoth.interpreter.ThothInterpreter;
import thoth.lang.ThothValue;

public class GotoInsn extends ThothInstruction {
    private final String destination;

    public GotoInsn(String destination) {
        super(Type.CONDITION);
        this.destination = destination;
    }

    public String getDestination() {
        return destination;
    }

    @Override
    public String execute(ThothValue[] params, InterpreterState state, ThothInterpreter interpreter) {
        interpreter.jump(destination);
        return "";
    }

    @Override
    public String toString() {
        return "GOTO "+destination;
    }
}
