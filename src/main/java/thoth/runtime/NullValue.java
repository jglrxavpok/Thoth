package thoth.runtime;

public class NullValue extends ThothValue<Void> {
    public NullValue() {
        super(null);
    }

    @Override
    public String convertToString() {
        return "null";
    }
}
