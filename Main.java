package strms.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Immutable record of a single event that occurred on a task.
 * Provides full traceability: what changed, when, and who performed it.
 */
public final class TaskHistoryEntry {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String        entryId;
    private final String        taskId;
    private final String        action;
    private final User          performedBy;
    private final LocalDateTime timestamp;
    private final String        details;

    public TaskHistoryEntry(String taskId, String action, User performedBy, String details) {
        this.entryId     = UUID.randomUUID().toString();
        this.taskId      = taskId;
        this.action      = action;
        this.performedBy = performedBy;
        this.timestamp   = LocalDateTime.now();
        this.details     = details;
    }

    // ── Getters ─────────────────────────────────────────────────────────────

    public String        getEntryId()     { return entryId; }
    public String        getTaskId()      { return taskId; }
    public String        getAction()      { return action; }
    public User          getPerformedBy() { return performedBy; }
    public LocalDateTime getTimestamp()   { return timestamp; }
    public String        getDetails()     { return details; }

    @Override
    public String toString() {
        String performer = (performedBy != null) ? performedBy.getName() : "SYSTEM";
        return String.format("[%s] %s by %s — %s",
                timestamp.format(FORMATTER), action, performer, details);
    }
}
