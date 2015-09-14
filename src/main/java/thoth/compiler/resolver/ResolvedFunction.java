package thoth.compiler.resolver;

import org.objectweb.asm.Label;
import thoth.compiler.ThothType;

import java.util.HashMap;
import java.util.Map;

public class ResolvedFunction {
    private final String[] argumentNames;
    private final ThothType[] types;
    private final String code;
    private final ResolvedClass owner;
    private final String name;
    public final Map<Integer, Label> labelMap;

    ResolvedFunction(String name, String[] argumentNames, ThothType[] types, String code, ResolvedClass owner) {
        this.name = name;
        this.argumentNames = argumentNames;
        this.types = types;
        this.code = code;
        this.owner = owner;
        labelMap = new HashMap<>();
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

    public void registerJump(int instructionNumber, Label label) {
        labelMap.put(instructionNumber, label);
    }
}
