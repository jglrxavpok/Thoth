package thoth.runtime;

public class TranslationValue extends ThothValue<Translation> {
    public TranslationValue(Translation value) {
        super(value);
    }

    @Override
    public String convertToString() {
        return getValue().getRaw();
    }
}
