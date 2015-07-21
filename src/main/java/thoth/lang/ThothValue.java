package thoth.lang;

public class ThothValue {

    public enum Types {
        BOOL, TEXT, TRANSLATION, NULL
    }

    private Object value;
    private final Types type;

    public ThothValue(Types type, Object value) {
        this.type = type;
        this.value = value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    public Types getType() {
        return type;
    }
}
