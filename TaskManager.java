package strms.exceptions;

/** Thrown when attempting to add a user with an ID that already exists. */
public class DuplicateUserException extends Exception {
    public DuplicateUserException(String message) { super(message); }
    public DuplicateUserException(String message, Throwable cause) { super(message, cause); }
}
