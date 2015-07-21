package thoth.interpreter;

import thoth.lang.ThothValue;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class InterpreterState {
    private final Stack<ThothValue> stack;
    private final Stack<Boolean> conditionStack;
    public String label;
    public int variable;
    private Map<String, Integer> jumpsDests;
    public int insnPointer;

    public InterpreterState() {
        stack = new Stack<>();
        conditionStack = new Stack<>();
        jumpsDests = new HashMap<>();
    }

    public void addJumpLocation(String label, int loc) {
        jumpsDests.put(label, loc);
    }

    public int getJumpLocation(String label) {
        if(jumpsDests.containsKey(label))
            return jumpsDests.get(label);
        return -1;
    }

    public void pushCondition(boolean value) {
        conditionStack.push(value);
    }

    public boolean peekCondition() {
        return conditionStack.peek();
    }

    public boolean popCondition() {
        return conditionStack.pop();
    }

    public void push(boolean value) {
        stack.push(new ThothValue(ThothValue.Types.BOOL, value));
    }

    public ThothValue pop() {
        return stack.pop();
    }

    public void push(ThothValue param) {
        stack.push(param);
    }
}
