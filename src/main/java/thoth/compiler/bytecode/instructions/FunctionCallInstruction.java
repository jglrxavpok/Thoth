package thoth.compiler.bytecode.instructions;

import thoth.compiler.resolver.ResolvedClass;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Instruction representing a function call. Contains info about the owner of the class, the name of the function and the argument count
 * (no types are required as Thoth is 'dynamic' for the end-user and uses ThothValue instances for everything under of the hood).
 */
public class FunctionCallInstruction extends ThothInstruction {

    /**
     * The owner of the function
     */
    private final ResolvedClass owner;

    /**
     * Function's name
     */
    private final String name;

    /**
     * Number of arguments required in order to call this method
     */
    private final int argumentCount;

    /**
     * Creates a new instance of FunctionCallInstruction
     * @param owner
     *             The owner of the function
     * @param name
     *            The name of the function
     * @param argumentCount
     *                      The argument count of the function
     */
    public FunctionCallInstruction(@Nonnull ResolvedClass owner, @Nonnull String name, int argumentCount) {
        super();
        this.owner = Objects.requireNonNull(owner);
        this.name = Objects.requireNonNull(name);
        this.argumentCount = argumentCount;
    }

    /**
     * Gets the owner of the function.
     * @return
     *         The function's owner
     */
    public ResolvedClass getOwner() {
        return owner;
    }

    /**
     * Returns the function name. Encoding is dependent on the compiler options.
     * @return
     *        The function name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the argument count, no types are required as Thoth is 'dynamic' for the end-user and uses ThothValue instances for everything under of the hood
     * @return
     *        The argument count
     */
    public int getArgumentCount() {
        return argumentCount;
    }

    /**
     * Generates a human readable String representation of this object
     * @return
     *        A String representation of this object
     */
    public String toString() {
        return "func_call "+owner.getName()+" "+name;
    }
}
