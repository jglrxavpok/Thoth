package org.jglr.thoth;

import java.util.Map;

public class ThothClass {
    private final Map<String, ThothFunc> functions;

    public ThothClass(Map<String, ThothFunc> functions) {
        this.functions = functions;
    }

    public ThothFunc getFunction(String name) {
        return functions.get(name);
    }
}
