package thoth.runtime;

public class TextValue extends ThothValue<String> {
    public TextValue(String value) {
        super(value);
    }

    @Override
    public String convertToString() {
        return getValue();
    }
}
