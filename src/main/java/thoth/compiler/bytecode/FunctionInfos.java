package thoth.compiler.bytecode;

import thoth.compiler.resolver.ResolvedClass;

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
