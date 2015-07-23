package thoth.insns;

import thoth.interpreter.InterpreterState;
import thoth.interpreter.ThothInterpreter;
import thoth.lang.ThothClass;
import thoth.lang.ThothFunc;
import thoth.lang.ThothValue;
import thoth.parser.FunctionCallDef;

public class FuncCallInsn extends ThothInstruction {
    private final FunctionCallDef func;

    public FuncCallInsn(FunctionCallDef def) {
        super(Type.CONDITION);
        this.func = def;
    }

    public FunctionCallDef getFunc() {
        return func;
    }

    @Override
    public String execute(ThothValue[] params, InterpreterState state, ThothInterpreter interpreter) {
        ThothClass thothClass = interpreter.getCurrentClass();
        ThothValue[] args = new ThothValue[func.nArgs];
        for(int i = args.length-1;i>=0;i--) {
            args[i] = state.pop();
        }
        ThothFunc function = null;
        if(func.isDirect()) {
            function = thothClass.getFunction(func.name);
        } else {
            function = thothClass.getFunction(params[func.fnameIndex].getValue().toString());
        }
        int insnPointer = state.insnPointer;
        String label = state.label;
        String result = interpreter.interpret(function, args);
        state.insnPointer = insnPointer;
        state.label = label;
        return result;
    }

    @Override
    public String toString() {
        return "FUNC "+func.name;
    }
}
