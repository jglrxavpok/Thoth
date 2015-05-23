package org.jglr.thoth;

public class BinUtils {

    public static boolean hasFlag(int value, int flag) {
        return (value & flag) != 0;
    }
}
