package thoth.lang;

import thoth.runtime.TextValue;
import thoth.runtime.ThothValue;
import thoth.runtime.Translation;

/**
 * Native functions from "thoth.lang.Operations.th"
 */
public final class DefaultOperations {

    public static ThothValue capitalize(ThothValue arg) {
        switch(arg.getType()) {
            case TEXT: {
                String value = (String) arg.getValue();
                String text = Character.toUpperCase(value.charAt(0)) + value.substring(1);
                return new TextValue(text);
            }

            case TRANSLATION: {
                Translation translation = (Translation) arg.getValue();
                String value = translation.getRaw();
                String text = Character.toUpperCase(value.charAt(0)) + value.substring(1);
                return new ThothValue(ThothValue.Types.TRANSLATION, text);
            }

            default:
                return arg;
        }
    }

    public static ThothValue uncapitalize(ThothValue arg) {
        switch(arg.getType()) {
            case TEXT: {
                String value = (String) arg.getValue();
                String text = Character.toLowerCase(value.charAt(0)) + value.substring(1);
                return new TextValue(text);
            }

            case TRANSLATION: {
                Translation translation = (Translation) arg.getValue();
                String value = translation.getRaw();
                String text = Character.toLowerCase(value.charAt(0)) + value.substring(1);
                return new ThothValue(ThothValue.Types.TRANSLATION, text);
            }

            default:
                return arg;
        }
    }

    public static ThothValue toUppercase(ThothValue arg) {
        switch(arg.getType()) {
            case TEXT: {
                String value = (String) arg.getValue();
                String text = value.toUpperCase();
                return new TextValue(text);
            }

            case TRANSLATION: {
                Translation translation = (Translation) arg.getValue();
                String value = translation.getRaw();
                String text = value.toUpperCase();
                return new ThothValue(ThothValue.Types.TRANSLATION, text);
            }

            default:
                return arg;
        }
    }

    public static ThothValue toLowerCase(ThothValue arg) {
        switch(arg.getType()) {
            case TEXT: {
                String value = (String) arg.getValue();
                String text = value.toLowerCase();
                return new TextValue(text);
            }

            case TRANSLATION: {
                Translation translation = (Translation) arg.getValue();
                String value = translation.getRaw();
                String text = value.toLowerCase();
                return new ThothValue(ThothValue.Types.TRANSLATION, text);
            }

            default:
                return arg;
        }
    }
}
