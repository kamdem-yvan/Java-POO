package model;

import user.User;

import java.time.LocalDateTime;
import java.util.UUID;

public class TaskHistoryEntry {

    private String entryId;
    private String taskId;

    private String action;

    private User performedBy;

    private LocalDateTime timestamp;

    private String details;

    public TaskHistoryEntry(String taskId,
                            String action,
                            User performedBy,
                            String details) {

        this.entryId = UUID.randomUUID().toString();

        this.taskId = taskId;

        this.action = action;

        this.performedBy = performedBy;

        this.details = details;

        this.timestamp = LocalDateTime.now();
    }

    // =========================
    // Getters
    // =========================

    public String getEntryId() {
        return entryId;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getAction() {
        return action;
    }

    public User getPerformedBy() {
        return performedBy;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getDetails() {
        return details;
    }

    // =========================
    // toString
    // =========================

    @Override
    public String toString() {

        return "TaskHistoryEntry{" +
                "entryId='" + entryId + '\'' +
                ", taskId='" + taskId + '\'' +
                ", action='" + action + '\'' +
                ", performedBy=" + performedBy.getName() +
                ", timestamp=" + timestamp +
                ", details='" + details + '\'' +
                '}';
    }
}