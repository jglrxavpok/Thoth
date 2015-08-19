package thoth.lang;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ThothClass {
    private final Map<String, ThothFunc> functions;
    private final List<ThothFunc> functionList;
    private final String sourceFile;
    private String name;

    public ThothClass(String name, String sourceFile, Map<String, ThothFunc> functions) {
        this.name = name.replace(".", "/");
        this.functions = functions;
        functionList = new ArrayList<>();
        functions.values().forEach(f -> {
            functionList.add(f);
            f.setClass(this);
        });
        this.sourceFile = sourceFile;
    }

    public String getSourceFile() {
        return sourceFile;
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
