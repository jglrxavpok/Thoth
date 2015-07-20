package org.jglr.thoth.parser;

import org.jglr.thoth.Translation;
import org.jglr.thoth.insns.ThothInstruction;

import java.util.List;

public class ThothFunc {

    private final List<String> argNames;
    private final List<ThothInstruction> instructions;
    private final Translation translation;
    private final String name;

    public ThothFunc(String name, List<String> args, List<ThothInstruction> instructions, Translation tr) {
        this.name = name;
        this.argNames = args;
        this.instructions = instructions;
        this.translation = tr;
    }

    public List<ThothInstruction> getInstructions() {
        return instructions;
    }

    public int getArgsNumber() {
        return argNames.size();
    }

    public List<String> getArgumentNames() {
        return argNames;
    }

    public Translation getTranslation() {
        return translation;
    }

    public String getName() {
        return name;
    }
}
