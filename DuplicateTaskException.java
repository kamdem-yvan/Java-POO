package strms.exceptions;

/** Thrown when a user attempts an action not permitted for their role. */
public class InvalidRoleException extends Exception {
    public InvalidRoleException(String message) { super(message); }
    public InvalidRoleException(String message, Throwable cause) { super(message, cause); }
}
