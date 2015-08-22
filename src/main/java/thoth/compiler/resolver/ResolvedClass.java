package thoth.compiler.resolver;

import thoth.compiler.ThothType;

import java.util.List;

public class ResolvedClass {

    private final String name;
    private final List<ResolvedClass> imports;
    private final List<ThothType> userTypes;
    private final List<ResolvedFunction> functions;
    private final boolean isTranslationSet;
    private final String sourceFile;

    ResolvedClass(String name, String sourceFile, List<ResolvedClass> imports, List<ThothType> userTypes, List<ResolvedFunction> functions, boolean isTranslationSet) {
        this.name = name;
        this.sourceFile = sourceFile;
        this.imports = imports;
        this.userTypes = userTypes;
        this.functions = functions;
        this.isTranslationSet = isTranslationSet;
    }

    public boolean isTranslationSet() {
        return isTranslationSet;
    }

    public List<ResolvedFunction> getFunctions() {
        return functions;
    }

    public String getName() {
        return name;
    }

    public List<ResolvedClass> getImports() {
        return imports;
    }

    public List<ThothType> getUserTypes() {
        return userTypes;
    }

    public String getSourceFile() {
        return sourceFile;
    }
}
