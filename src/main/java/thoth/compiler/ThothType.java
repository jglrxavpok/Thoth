package thoth.compiler;

public class ThothType {

    private final String name;
    private final String shorthand;

    public ThothType(String name, String shorthand) {
        this.name = name;
        this.shorthand = shorthand;
    }

    public String getName() {
        return name;
    }

    public String getShorthand() {
        return shorthand;
    }

    public boolean equals(Object o) {
        if(o == null)
            return false;
        if(o == this)
            return true;
        if(o instanceof ThothType) {
            ThothType type = (ThothType)o;
            return name.equals(type.getName()); // No need to verify shorthand, types should collide even if their shorthand are different.
        }
        return false;
    }

}
