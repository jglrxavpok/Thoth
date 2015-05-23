package org.jglr.thoth.insns;

import org.jglr.thoth.*;

public class FlagInstruction extends ThothCommandment {
    private final int flags;
    private final String baseParam;

    public FlagInstruction(String param) {
        super(Type.VARIABLE);
        this.baseParam = param;
        int typeFlag = 0;
        for(int j = 0;j<param.length();j++) {
            char c = param.charAt(j);
            if(Constants.idsToFlag.containsKey(c)) {
                int flag = Constants.idsToFlag.get(c);
                typeFlag |= flag;
            }
        }
        if(typeFlag == 0) {
            typeFlag = Constants.FLAG_NEUTRAL;
        }
        this.flags = typeFlag;
    }

    @Override
    public String execute(ThothValue[] params, InterpreterState state, ThothInterpreter interpreter) {
        ThothValue usedParam = params[state.variable];
        if(usedParam.getType() == ThothValue.Types.TRANSLATION) {
            Translation tr = (Translation) usedParam.getValue();
            state.push((tr.getFlags() & flags) != 0);
        } else {
            state.push(usedParam.getType() != ThothValue.Types.NULL);
        }
        // TODO: Return content
        return "";
    }

    @Override
    public String toString() {
        return "PARAM "+baseParam;
    }
}
