package thoth.compiler.parser;

import thoth.compiler.ThothType;

import java.util.List;

public class ParsedClass {

    private final String name;
    private final String sourceFile;
    private final List<String> imports;
    private final List<ThothType> userTypes;
    private final List<ParsedFunction> functions;
    private final boolean isTranslationSet;

    ParsedClass(String name, String sourceFile, List<String> imports, List<ThothType> userTypes, List<ParsedFunction> functions, boolean isTranslationSet) {
        this.isTranslationSet = isTranslationSet;
        this.name = name;
        this.sourceFile = sourceFile;
        this.imports = imports;
        this.userTypes = userTypes;
        this.functions = functions;
    }

    public boolean isTranslationSet() {
        return isTranslationSet;
    }

    public String getName() {
        return name;
    }

    public List<String> getImports() {
        return imports;
    }

    public List<ThothType> getUserTypes() {
        return userTypes;
    }

    public List<ParsedFunction> getFunctions() {
        return functions;
    }

    public String rebuildSource() {
        StringBuilder builder = new StringBuilder();
        builder.append("class ").append(name).append("\n");

        for(String imported : imports) {
            builder.append("import ").append(imported).append("\n");
        }

        for(ThothType type : userTypes) {
            builder.append("import ").append(type.getName()).append(" ").append(type.getShorthand()).append("\n");
        }

        for(ParsedFunction func : getFunctions()) {
            builder.append("def ").append(func.getName()).append("(");
            for(int i = 0;i<func.getArgumentNames().length;i++) {
                String arg = func.getArgumentNames()[i];
                if(i != 0)
                    builder.append(", ");
                builder.append(arg);
            }
            builder.append(")").append(":");
            for(int i = 0;i<func.getTypes().length;i++) {
                String type = func.getTypes()[i];
                if(i != 0)
                    builder.append("&");
                builder.append(type);
            }
            builder.append(" = ").append(func.getCode()).append("\n");
        }

        return builder.toString();
    }

    public String getSourceFile() {
        return sourceFile;
    }
}
