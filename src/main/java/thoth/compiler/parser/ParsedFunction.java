package thoth.compiler.parser;

public class ParsedFunction {

    private final String[] argumentNames;
    private final String[] types;
    private final String code;
    private final String name;

    ParsedFunction(String name, String[] argumentNames, String[] types, String code) {
        this.name = name;
        this.argumentNames = argumentNames;
        this.types = types;
        this.code = code;
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

    public String[] getTypes() {
        return types;
    }
}
