package thoth.insns;

import thoth.lang.ThothValue;
import thoth.lang.Translation;
import thoth.interpreter.InterpreterState;
import thoth.interpreter.ThothInterpreter;
import thoth.Constants;

public class FlagInstruction extends ThothInstruction {
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

    public int getFlags() {
        return flags;
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
        return "";
    }

    @Override
    public String toString() {
        return "FLAG "+baseParam;
    }
}
