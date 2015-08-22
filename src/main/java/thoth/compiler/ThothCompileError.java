package thoth.compiler;

public class ThothCompileError extends Error {

    private final String message;
    private final int line;
    private final int column;

    public ThothCompileError(String message, int line, int column) {
        super(message+" (at line "+line+", column "+column+")");
        this.message = message;
        this.line = line;
        this.column = column;
    }

    public String getRawMessage() {
        return message;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }
}
