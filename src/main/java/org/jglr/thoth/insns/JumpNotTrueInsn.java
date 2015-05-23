package org.jglr.thoth.insns;

import org.jglr.thoth.ThothInterpreter;
import org.jglr.thoth.ThothValue;
import org.jglr.thoth.InterpreterState;

public class JumpNotTrueInsn extends ThothCommandment {
    private final String destination;

    public JumpNotTrueInsn(String destination) {
        super(Type.CONDITION);
        this.destination = destination;
    }

    @Override
    public String execute(ThothValue[] params, InterpreterState state, ThothInterpreter interpreter) {
        if(!state.peekCondition()) {
            interpreter.jump(destination);
        }
        return "";
    }

    @Override
    public String toString() {
        return "JUMP IF NOT "+destination;
    }
}
