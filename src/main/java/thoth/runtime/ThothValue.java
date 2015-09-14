package thoth.runtime;

public abstract class ThothValue<T> {

    private T value;

    public ThothValue(T value) {
        this.value = value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    public abstract String convertToString();

    public abstract boolean convertToBoolean();
}
