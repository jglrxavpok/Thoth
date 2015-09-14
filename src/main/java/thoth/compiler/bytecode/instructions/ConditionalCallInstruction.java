package thoth.compiler.bytecode.instructions;

public class ConditionalCallInstruction extends ThothInstruction {

    private FunctionCallInstruction call;
    public int instructionNumber = -1;

    public ConditionalCallInstruction() {
    }

    @Override
    public String toString() {
        return "callif "+call;
    }

    public FunctionCallInstruction getCall() {
        return call;
    }

    public void setCall(FunctionCallInstruction call) {
        this.call = call;
    }
}
