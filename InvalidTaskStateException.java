package strms.exceptions;

/** Thrown when a referenced user does not exist in the system. */
public class UserNotFoundException extends Exception {
    public UserNotFoundException(String message) { super(message); }
    public UserNotFoundException(String message, Throwable cause) { super(message, cause); }
}
