package thoth.lang;

import thoth.runtime.TextValue;
import thoth.runtime.ThothValue;
import thoth.runtime.Translation;

/**
 * Native functions from "thoth.lang.Operations.th"
 */
public final class DefaultOperations {

    public static ThothValue capitalize(ThothValue arg) {
        String value = arg.convertToString();
        String text = Character.toUpperCase(value.charAt(0)) + value.substring(1);
        return new TextValue(text);
    }

    public static ThothValue uncapitalize(ThothValue arg) {
        String value = arg.convertToString();
        String text = Character.toLowerCase(value.charAt(0)) + value.substring(1);
        return new TextValue(text);
    }

    public static ThothValue toUpperCase(ThothValue arg) {
        return new TextValue(arg.convertToString().toUpperCase());
    }

    public static ThothValue toLowerCase(ThothValue arg) {
        return new TextValue(arg.convertToString().toLowerCase());
    }
}
