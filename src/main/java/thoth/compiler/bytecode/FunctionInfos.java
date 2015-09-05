package thoth.compiler.bytecode;

import thoth.compiler.resolver.ResolvedClass;

/**
 * <h1>Data class</h1>
 * Container for a function owner and an argument count.
 */
class FunctionInfos {
    private final ResolvedClass owner;
    private final int argCount;

    FunctionInfos(ResolvedClass owner, int argCount) {
        this.owner = owner;
        this.argCount = argCount;
    }

    public ResolvedClass getOwner() {
        return owner;
    }

    public int getArgCount() {
        return argCount;
    }
}
