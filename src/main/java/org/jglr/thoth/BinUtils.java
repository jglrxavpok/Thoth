package org.jglr.thoth;

public class BinUtils {

    /**
     * Checks if <code>value</code> has given flag
     * @param value
     *      The value to check
     * @param flag
     *      The flag to search
     * @return
     *      <code>true</code> if <code>value</code> has given flag, <code>false</code> otherwise
     */
    public static boolean hasFlag(int value, int flag) {
        return (value & flag) != 0;
    }
}
