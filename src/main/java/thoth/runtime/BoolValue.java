package thoth.runtime;

public class BoolValue extends ThothValue<Boolean> {
    public BoolValue(boolean value) {
        super(value);
    }

    @Override
    public String convertToString() {
        return String.valueOf(getValue());
    }

    @Override
    public boolean convertToBoolean() {
        return getValue();
    }
}
