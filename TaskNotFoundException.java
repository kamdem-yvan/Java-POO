package strms.exceptions;

/**
 * Thrown when a user attempts to start a task whose dependencies are not yet completed.
 */
public class DependencyNotCompletedException extends Exception {
    public DependencyNotCompletedException(String message) {
        super(message);
    }
    public DependencyNotCompletedException(String message, Throwable cause) {
        super(message, cause);
    }
}
