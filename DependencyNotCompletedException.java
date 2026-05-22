package strms.exceptions;

/** Thrown when attempting to create a task with an ID that already exists. */
public class DuplicateTaskException extends Exception {
    public DuplicateTaskException(String message) { super(message); }
    public DuplicateTaskException(String message, Throwable cause) { super(message, cause); }
}
