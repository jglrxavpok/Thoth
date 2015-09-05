package thoth.compiler;

public class CompilerOptions {
    private final boolean mutable;
    private int bufferSize;
    private String encoding;

    private CompilerOptions(boolean mutable, CompilerOptions options) {
        this.mutable = mutable;
        encoding = "UTF-8";
        bufferSize = 4096;
        if(options != null) {
            withEncoding(options.getEncoding());
        }
    }

    public CompilerOptions withEncoding(String encoding) {
        checkMutable();
        this.encoding = encoding;
        return this;
    }

    public CompilerOptions withBufferSize(int bufferSize) {
        checkMutable();
        if(bufferSize < 0)
            throw new IllegalArgumentException("bufferSize cannot be negative");
        this.bufferSize = bufferSize;
        return this;
    }

    private void checkMutable() {
        if(!mutable)
            throw new IllegalStateException("Tried to change state while instance is immutable");
    }

    public String getEncoding() {
        return encoding;
    }

    public static CompilerOptions copyDefault() {
        return new CompilerOptions(true, null);
    }

    public static CompilerOptions immutableVersion(CompilerOptions options) {
        CompilerOptions immutable = new CompilerOptions(false, options);
        return immutable;
    }

    public int getBufferSize() {
        return bufferSize;
    }
}
