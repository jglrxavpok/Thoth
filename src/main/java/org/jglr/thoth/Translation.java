package org.jglr.thoth;

public class Translation implements Constants {

    private final boolean feminine;
    private final boolean masculine;
    private final boolean neutral;
    private final boolean plural;
    private final boolean singular;
    private final String rawCode;
    private final String[] params;

    private int flags;

    public Translation(int flags, String rawCode, String[] params) {
        this.flags = flags;
        this.rawCode = rawCode;
        this.params = params;
        feminine = BinUtils.hasFlag(flags, FLAG_FEMININE);
        masculine = BinUtils.hasFlag(flags, FLAG_MASCULINE);
        neutral = BinUtils.hasFlag(flags, FLAG_NEUTRAL);
        singular = BinUtils.hasFlag(flags, FLAG_SINGULAR);
        plural = BinUtils.hasFlag(flags, FLAG_PLURAL);
    }

    public boolean isFeminine() {
        return feminine;
    }

    public boolean isMasculine() {
        return masculine;
    }

    public boolean isNeutral() {
        return neutral;
    }

    public boolean isPlural() {
        return plural;
    }

    public boolean isSingular() {
        return singular;
    }

    public int getFlags() {
        return flags;
    }

    public String getRawCode() {
        return rawCode;
    }

    public String[] getParams() {
        return params;
    }
}