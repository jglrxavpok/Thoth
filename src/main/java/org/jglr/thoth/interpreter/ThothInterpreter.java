package org.jglr.thoth.interpreter;

import org.jglr.thoth.NullValue;
import org.jglr.thoth.parser.ThothFunc;
import org.jglr.thoth.ThothValue;
import org.jglr.thoth.insns.LabelInstruction;
import org.jglr.thoth.insns.ThothInstruction;

import java.util.List;

public class ThothInterpreter {

    private final InterpreterState state;

    public ThothInterpreter() {
        this.state = new InterpreterState();
    }

    /**
     * Executes <code>function</code> and returns the result.
     * @param function
     *      The function to execute
     * @param params
     *      The values given as parameters
     * @return
     *      The result of the function call
     */
    public String interpret(ThothFunc function, ThothValue... params) {
        ThothValue[] actualParams = new ThothValue[function.getArgsNumber()];
        int min = Math.min(actualParams.length,params.length);
        for(int i = 0;i<min;i++) { // Try to fill the actualParams array with the given parameters
            if (params[i] != null) {
                actualParams[i] = params[i];
            } else {
                actualParams[i] = new NullValue();
            }
        }

        for(int i = min;i<actualParams.length;i++) { // Finish filling the actualParams array with nulls if needed
            actualParams[i] = new NullValue();
        }

        List<ThothInstruction> insns = function.getInstructions();
        computeJumpsDestinations(insns);
        state.insnPointer = 0;
        StringBuilder builder = new StringBuilder();
        for(;state.insnPointer<insns.size();state.insnPointer++) {
            if(state.insnPointer < 0) {
                throw new RuntimeException("Tried to jump to wrong label: "+state.label);
            }
            ThothInstruction insn = insns.get(state.insnPointer);
            String partialResult = insn.execute(actualParams, state, this);
            builder.append(partialResult);
        }
        return builder.toString();
    }

    /**
     * Generates the destination to which jump for every label instruction
     * @param insns
     *      The list of instructions in which to search for labels
     */
    private void computeJumpsDestinations(List<ThothInstruction> insns) {
        for(int i = 0;i<insns.size();i++) {
            ThothInstruction insn = insns.get(i);
            System.out.println(insn);
            if(insn.getType() == ThothInstruction.Type.LABEL) {
                LabelInstruction labelInsn = (LabelInstruction)insn;
                state.addJumpLocation(labelInsn.getLabel(), i);
            }
        }
    }

    /**
     * Jump to the given destination
     * @param destination
     *      The label to which jump
     */
    public void jump(String destination) {
        state.insnPointer = state.getJumpLocation(destination);
        state.label = destination;
    }
}
