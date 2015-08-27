package thoth.runtime;

public class SpaceValue extends TextValue {

    private static final SpaceValue instance = new SpaceValue();

    public SpaceValue() {
        super(" ");
    }

    public static SpaceValue getInstance() {
        return instance;
    }
}
