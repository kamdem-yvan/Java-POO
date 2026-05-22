package strms.exceptions;

/** Thrown when a requested task identifier does not exist in the system. */
public class TaskNotFoundException extends Exception {
    public TaskNotFoundException(String message) { super(message); }
    public TaskNotFoundException(String message, Throwable cause) { super(message, cause); }
}
