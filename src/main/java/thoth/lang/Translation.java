package thoth.lang;

import thoth.Constants;
import thoth.Utils;

public class Translation implements Constants {

    private final boolean feminine;
    private final boolean masculine;
    private final boolean neutral;
    private final boolean plural;
    private final boolean singular;
    private String raw;
    private final String[] params;

    private int flags;

    public Translation(int flags, String raw, String[] params) {
        this.flags = flags;
        this.raw = raw;
        this.params = params;
        feminine = Utils.hasFlag(flags, FLAG_FEMININE);
        masculine = Utils.hasFlag(flags, FLAG_MASCULINE);
        neutral = Utils.hasFlag(flags, FLAG_NEUTRAL);
        singular = Utils.hasFlag(flags, FLAG_SINGULAR);
        plural = Utils.hasFlag(flags, FLAG_PLURAL);
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

    public void setRaw(String raw) {
        this.raw = raw;
    }

    public String getRaw() {
        return raw;
    }

    public String[] getParams() {
        return params;
    }

    public String toString() {
        return getRaw();
    }
}
