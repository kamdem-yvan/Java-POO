package strms.enums;

/**
 * Represents the current state of a task within the STRMS.
 * State transitions: TODO -> BLOCKED -> IN_PROGRESS -> DONE
 */
public enum TaskStatus {
    TODO,
    BLOCKED,
    IN_PROGRESS,
    DONE
}
