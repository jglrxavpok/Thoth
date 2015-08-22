package thoth.compiler;

import java.util.LinkedList;
import java.util.List;

public abstract class ThothCompilePhase {
    private final List<ThothCompileError> errors;
    private final List<ThothWarning> warnings;

    public ThothCompilePhase() {
        errors = new LinkedList<>();
        warnings = new LinkedList<>();
    }

    public List<ThothCompileError> getErrors() {
        return errors;
    }

    public List<ThothWarning> getWarnings() {
        return warnings;
    }

    protected void newError(String message, int line, int column) {
        errors.add(new ThothCompileError(message, line, column));
    }

    protected void newWarning(String warningClass, String message, int line, int column) {
        if(isAuthorized(warningClass))
            warnings.add(new ThothWarning(message, line, column));
    }

    protected boolean isAuthorized(String warningClass) {
        return true; // TODO: Allow for custom warning toggling
    }

}
