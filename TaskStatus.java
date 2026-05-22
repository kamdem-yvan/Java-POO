package strms.enums;

/**
 * Defines the urgency and importance of a task.
 * Used in task scheduling and priority-based task selection.
 */
public enum PriorityLevel {
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    CRITICAL(4);

    private final int value;

    PriorityLevel(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
