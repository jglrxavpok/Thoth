package thoth.lang;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ThothClass {
    private final Map<String, ThothFunc> functions;
    private final List<ThothFunc> functionList;
    private String name;

    public ThothClass(String name, Map<String, ThothFunc> functions) {
        this.name = name.replace(".", "/");
        this.functions = functions;
        functionList = new ArrayList<>();
        functions.values().forEach(functionList::add);
    }

    public ThothFunc getFunction(String name) {
        return functions.get(name);
    }

    public List<ThothFunc> getFunctions() {
        return functionList;
    }

    public String getName() {
        return name;
    }
}
