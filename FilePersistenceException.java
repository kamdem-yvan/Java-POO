package strms.exceptions;

/** Thrown when an invalid state transition is attempted (e.g., DONE -> IN_PROGRESS). */
public class InvalidTaskStateException extends Exception {
    public InvalidTaskStateException(String message) { super(message); }
    public InvalidTaskStateException(String message, Throwable cause) { super(message, cause); }
}
