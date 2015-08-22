package thoth.compiler.bytecode.instructions;

import thoth.compiler.resolver.ResolvedFunction;

public class NativeCallInstruction extends ThothInstruction {

    private final String nativeLocation;
    private final ResolvedFunction caller;

    public NativeCallInstruction(ResolvedFunction caller, String nativeLocation) {
        this.caller = caller;
        this.nativeLocation = nativeLocation;
    }

    public String getNativeLocation() {
        return nativeLocation;
    }

    public ResolvedFunction getCaller() {
        return caller;
    }

    public String toString() {
        return "native(\""+caller.getName()+"\") ["+nativeLocation+"]";
    }
}
