package thoth.runtime;

public class TextValue extends ThothValue {
    public TextValue(String value) {
        super(Types.TEXT, value);
    }
}
