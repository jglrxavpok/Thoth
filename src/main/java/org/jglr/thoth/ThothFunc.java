package org.jglr.thoth;

import org.jglr.thoth.insns.ThothCommandment;

import java.util.List;

public class ThothFunc {

    private final int argsNumber;
    private final List<ThothCommandment> instructions;
    private final Translation translation;

    public ThothFunc(int argsNumber, List<ThothCommandment> instructions, Translation tr) {
        this.argsNumber = argsNumber;
        this.instructions = instructions;
        this.translation = tr;
    }

    public List<ThothCommandment> getInstructions() {
        return instructions;
    }

    public int getArgsNumber() {
        return argsNumber;
    }

    public Translation getTranslation() {
        return translation;
    }
}
