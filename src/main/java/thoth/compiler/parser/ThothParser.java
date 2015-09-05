package thoth.compiler.parser;

import thoth.compiler.*;
import thoth.runtime.ClassType;

import java.util.LinkedList;
import java.util.List;

/**
 * ThothParser's role is to parse Thoth code, that's to say find possible errors and warnings in the code.
 * It also extracts informations from it such as imports, class name, function definitions.
 * It does NOT compile in any way.<br/>
 * For instance, with the following code:<br/>
 * <code>
 * class ClassA<br/>
 * <br/>
 * import ClassB<br/>
 * <br/>
 * def functionA(arg)= "a" arg<br/>
 * </code><br/>
 * the parser will extract that the class name is "ClassA", that this file imports from "ClassB" and that it has a function called "functionA" containing the code
 * {@code "a" arg}.
 */
public class ThothParser extends ThothCompilePhase {

    private final List<String> imports;
    private final List<ThothType> userTypes;
    private final List<ParsedFunction> functions;
    private String code;
    private String sourceFile;
    private CompilerOptions options;
    private int index;
    private char[] chars;
    private boolean eof;
    private int line;
    private int column;
    private String currentClassName;
    private ParsedClass parsedClass;
    private ClassType classType;

    public ThothParser(String code, String sourceFile) {
        this(code, sourceFile, CompilerOptions.copyDefault());
    }

    public ThothParser(String code, String sourceFile, CompilerOptions options) {
        this.code = code;
        this.sourceFile = sourceFile;
        this.options = options;
        chars = code.toCharArray();
        imports = new LinkedList<>();
        userTypes = new LinkedList<>();
        functions = new LinkedList<>();
        line = 1;
    }

    public void parse() {
        while(!eof) {
            /*readUntilNot(' ', '\n');
            index--; // TODO: Check if works*/
            String start = readUntil(' ', '\n');
            switch (start) {

                case "language": {
                    String read = readUntil(' ', '\n');
                    if (!read.equals("class")) {
                        newError("Expected class keyword, found " + read);
                    }
                    classType = ClassType.LANGUAGE;
                    readClassName();
                }
                    break;

                case "utility": {
                    String read = readUntil(' ', '\n');
                    if (!read.equals("class")) {
                        newError("Expected class keyword, found " + read);
                    }
                    classType = ClassType.UTILIY;
                    readClassName();
                }
                    break;

                case "class":
                    classType = ClassType.TRANSLATION_SET;
                    readClassName();
                    break;

                case "import":
                    readImport();
                    break;

                case "typedef":
                    readTypeDefinition();
                    break;

                case "def":
                    readFunctionDeclaration();
                    break;

                case "":
                case "\n":
                case "\r":
                    // Ignore
                    break;

                default:
                    if(start.startsWith("//")) {
                        readUntil('\n');
                    } else if(start.startsWith("/*")) {
                        readUntil("*/");
                    } else {
                        newError("Cannot resolve " + start);
                    }
                    break;
            }
        }
        // Compile results inside a ParsedClass instance
        if(currentClassName == null) {
            newError("Class name not found");
        } else {
            parsedClass = new ParsedClass(getClassName(), sourceFile, getImports(), getUserDefinedTypes(), getFunctions(), classType);
        }
    }

    public ParsedClass getParsedClass() {
        return parsedClass;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public String getCode() {
        return code;
    }

    public String getClassName() {
        return currentClassName;
    }

    public List<String> getImports() {
        return imports;
    }

    public List<ThothType> getUserDefinedTypes() {
        return userTypes;
    }

    public List<ParsedFunction> getFunctions() {
        return functions;
    }

    private void readFunctionDeclaration() {
        String functionName = readUntil('(', '=', ':', '\n').replace(" ", "");
        char prev = chars[index-1];
        String functionCode = "";
        String[] arguments = new String[0];
        String[] types = new String[0];
        switch (prev) {
            case '(':
                arguments = readArguments();
                String s = readUntil('=', ':', '\n');
                char prev1 = chars[index-1];
                if(prev1 == ':') {
                    types = readTypes();
                    functionCode = readCode();
                } else if(prev1 == '=') {
                    functionCode = readCode();
                }
                break;

            case ':':
                types = readTypes();
                functionCode = readCode();
                break;

            case ' ':
            case '\n':
                // Ignore
                break;

            case '=':
                functionCode = readCode();
                break;
        }

        functions.add(new ParsedFunction(functionName, arguments, types, functionCode));
    }

    private String readCode() {
        readUntilNot(' ');
        index--;
        int numBlocks = 0;
        StringBuilder builder = new StringBuilder();
        for(;index<code.length();index++) {
            char read = chars[index];
            if(read == '{') {
                numBlocks++;
            } else if(read == '}') {
                numBlocks--;
            } else if(read == '\n') {
                if(numBlocks == 0) { // We've reached end of function code
                    break;
                }
            }
            builder.append(read);
        }
        return builder.toString();
    }

    private String[] readTypes() {
        String type = readUntil('=').replace(" ", "");
        String[] parts = type.split("&");
        return parts;
    }

    private String[] readArguments() {
        String raw = readUntil(')');
        String[] args = raw.split(",");
        for(int i = 0;i<args.length;i++) {
            args[i] = args[i].replace(" ", "");
        }
        return args;
    }

    private void readTypeDefinition() {
        String typeName = readUntil(' ', '\n');
        if(!errorIfEOF()) {
            String shorthand = readUntil(' ', '\n');
            // TODO: Verify if type does not already exist
            userTypes.add(new ThothType(typeName, shorthand));
        }
    }

    private void readClassName() {
        String className = readUntil(' ', '\n');
        if(validateClassName(className)) {
            if (currentClassName != null) {
                newWarning("redefining.class", "Redefining class name, surely an error, please check.");
            }
            currentClassName = className;
        }
    }

    private void readImport() {
        String className = readUntil(' ', '\n');
        if(validateClassName(className)) {
            if (imports.contains(className)) {
                newWarning("redefining.import", className + " has already been imported");
            }
            imports.add(className);
        }
    }

    private boolean validateClassName(String className) {
        if(className.isEmpty()) { // TODO: Full validation of name
            newError("Invalid className: " + className);
            return false;
        }
        return true;
    }

    private boolean errorIfEOF() {
        if(eof) {
            newError("Unexpected end of file");
            return true;
        }
        return false;
    }

    private boolean warningIfEOF() {
        if(eof) {
            newWarning("unexpected.eof", "Unexpected end of file while reading, probably an error. If not, please add a line return at the end of the file.");
            return true;
        }
        return false;
    }

    private void newError(String message) {
        newError(message, line, column);
    }

    private void newWarning(String warningClass, String message) {
        newWarning(warningClass, message, line, column);
    }

    private String readUntilValue(boolean value, char... end) {
        StringBuilder builder = new StringBuilder();
        boolean endFound = false;
        for(;index < code.length() && !endFound;index++) {
            char read = chars[index];
            if(read == '\n') {
                line++;
                column = 0;
            } else {
                column++;
            }
            for(char c : end) {
                boolean condition = c == read;
                if(condition == value) {
                    endFound = true;
                    break;
                }
            }
            if(!endFound)
                builder.append(read);
        }
        if(index >= code.length()) {
            eof = true;
        }
        return builder.toString();
    }

    private String readUntil(String str) {
        StringBuilder builder = new StringBuilder();
        boolean endFound = false;
        for(;index < code.length() && !endFound;index++) {
            char read = chars[index];
            if(read == '\n') {
                line++;
                column = 0;
            } else {
                column++;
            }
            if(code.indexOf(str, index) == index) {
                endFound = true;
            }
            if(!endFound)
                builder.append(read);
        }
        index += str.length()-1;
        if(index >= code.length()) {
            eof = true;
        }
        return builder.toString();
    }

    private String readUntil(char... end) {
        return readUntilValue(true, end);
    }

    private String readUntilNot(char... end) {
        return readUntilValue(false, end);
    }

}
