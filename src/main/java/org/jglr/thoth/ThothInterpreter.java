package org.jglr.thoth;

import org.jglr.thoth.insns.LabelInstruction;
import org.jglr.thoth.insns.ThothCommandment;

import java.util.List;

public class ThothInterpreter {

    private final InterpreterState state;

    public ThothInterpreter() {
        this.state = new InterpreterState();
    }

    public String interpret(ThothFunc function, ThothValue... params) {
        ThothValue[] actualParams = new ThothValue[function.getArgsNumber()];
        int min = Math.min(actualParams.length,params.length);
        for(int i = 0;i<min;i++) {
            if (params[i] != null) {
                actualParams[i] = params[i];
            } else {
                actualParams[i] = new NullValue();
            }
        }

        for(int i = min;i<actualParams.length;i++) {
            actualParams[i] = new NullValue();
        }

        List<ThothCommandment> insns = function.getInstructions();
        computeJumpsDestinations(insns);
        state.insnPointer = 0;
        StringBuilder builder = new StringBuilder();
        for(;state.insnPointer<insns.size();state.insnPointer++) {
            if(state.insnPointer < 0) {
                throw new RuntimeException("Tried to jump to wrong label: "+state.label);
            }
            ThothCommandment insn = insns.get(state.insnPointer);
            builder.append(insn.execute(actualParams, state, this));
        }
        return builder.toString();
    }

    private void computeJumpsDestinations(List<ThothCommandment> insns) {
        for(int i = 0;i<insns.size();i++) {
            ThothCommandment insn = insns.get(i);
            System.out.println(insn);
            if(insn.getType() == ThothCommandment.Type.LABEL) {
                LabelInstruction labelInsn = (LabelInstruction)insn;
                state.addJumpLocation(labelInsn.getLabel(), i);
            }
        }
    }

    public void jump(String destination) {
        state.insnPointer = state.getJumpLocation(destination);
        state.label = destination;
    }
}
