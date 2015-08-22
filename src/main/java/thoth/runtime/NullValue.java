package thoth.runtime;

public class NullValue extends ThothValue {
    public NullValue() {
        super(Types.NULL, "null");
    }
}
