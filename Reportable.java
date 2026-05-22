package strms.exceptions;

/**
 * Thrown when a new dependency would create a circular dependency among tasks.
 */
public class CircularDependencyException extends Exception {
    public CircularDependencyException(String message) {
        super(message);
    }
    public CircularDependencyException(String message, Throwable cause) {
        super(message, cause);
    }
}
