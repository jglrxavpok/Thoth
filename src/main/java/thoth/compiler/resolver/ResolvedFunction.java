package thoth.compiler.resolver;

import thoth.compiler.ThothType;

public class ResolvedFunction {
    private final String[] argumentNames;
    private final ThothType[] types;
    private final String code;
    private final ResolvedClass owner;
    private final String name;

    ResolvedFunction(String name, String[] argumentNames, ThothType[] types, String code, ResolvedClass owner) {
        this.name = name;
        this.argumentNames = argumentNames;
        this.types = types;
        this.code = code;
        this.owner = owner;
    }

    public ResolvedClass getOwner() {
        return owner;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String[] getArgumentNames() {
        return argumentNames;
    }

    public ThothType[] getTypes() {
        return types;
    }

    public int getArgumentCount() {
        return argumentNames.length;
    }
}
