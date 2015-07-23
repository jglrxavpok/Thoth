package thoth.parser;

public class FunctionCallDef {
    public final String name;
    public final int nArgs;
    public final int nChars;
    private final boolean direct;
    public final int fnameIndex;

    public FunctionCallDef(String name, int nArgs, int nChars, boolean direct, int fnameIndex) {
        this.name = name;
        this.nArgs = nArgs;
        this.nChars = nChars;
        System.out.println(">> "+nChars);
        this.direct = direct;
        this.fnameIndex = fnameIndex;
    }

    public boolean isDirect() {
        return direct;
    }
}
