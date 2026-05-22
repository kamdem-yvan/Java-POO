package strms.exceptions;

/** Thrown when a read or write operation to a file fails. */
public class FilePersistenceException extends Exception {
    public FilePersistenceException(String message) { super(message); }
    public FilePersistenceException(String message, Throwable cause) { super(message, cause); }
}
