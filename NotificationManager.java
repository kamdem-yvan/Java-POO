package strms.model;

import strms.enums.PriorityLevel;
import strms.enums.TaskCategory;
import strms.enums.TaskStatus;
import strms.exceptions.InvalidTaskStateException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single unit of work in the STRMS.
 * Encapsulates its own data (ID, title, status, priority, etc.),
 * maintains a list of prerequisite tasks (dependencies),
 * and keeps a chronological history of all events.
 *
 * Implements Comparable to allow priority-based ordering in a PriorityQueue
 * (higher PriorityLevel value = higher urgency = comes first).
 */
public class Task implements Comparable<Task> {

    private final String            taskId;
    private String                  title;
    private String                  description;
    private PriorityLevel           priority;
    private TaskStatus              status;
    private TaskCategory            category;
    private LocalDate               deadline;
    private final LocalDateTime     creationDate;
    private Engineer                assignedEngineer;

    /** Tasks that must be DONE before this task can start. */
    private final List<Task>             dependencies;
    /** Full chronological audit trail for this task. */
    private final List<TaskHistoryEntry> history;

    // ── Constructor ─────────────────────────────────────────────────────────

    public Task(String taskId, String title, String description,
                PriorityLevel priority, TaskCategory category, LocalDate deadline) {
        this.taskId       = taskId;
        this.title        = title;
        this.description  = description;
        this.priority     = priority;
        this.category     = category;
        this.deadline     = deadline;
        this.status       = TaskStatus.TODO;
        this.creationDate = LocalDateTime.now();
        this.dependencies = new ArrayList<>();
        this.history      = new ArrayList<>();
    }

    // ── Getters & Setters ───────────────────────────────────────────────────

    public String        getTaskId()          { return taskId; }
    public String        getTitle()           { return title; }
    public void          setTitle(String t)   { this.title = t; }
    public String        getDescription()     { return description; }
    public void          setDescription(String d) { this.description = d; }
    public PriorityLevel getPriority()        { return priority; }
    public void          setPriority(PriorityLevel p) { this.priority = p; }
    public TaskStatus    getStatus()          { return status; }
    public TaskCategory  getCategory()        { return category; }
    public void          setCategory(TaskCategory c) { this.category = c; }
    public LocalDate     getDeadline()        { return deadline; }
    public void          setDeadline(LocalDate d) { this.deadline = d; }
    public LocalDateTime getCreationDate()    { return creationDate; }
    public Engineer      getAssignedEngineer() { return assignedEngineer; }
    public void          setAssignedEngineer(Engineer e) { this.assignedEngineer = e; }

    // ── Dependencies ────────────────────────────────────────────────────────

    public List<Task> getDependencies() { return dependencies; }

    /**
     * Adds a prerequisite task. Does not add duplicates or self-references.
     * @return true if added, false if already present or self-reference
     */
    public boolean addDependency(Task task) {
        if (task == null || task.getTaskId().equals(this.taskId)) return false;
        if (dependencies.contains(task)) return false;
        dependencies.add(task);
        return true;
    }

    /** Removes a dependency. @return true if removed, false if not found */
    public boolean removeDependency(Task task) {
        return dependencies.remove(task);
    }

    public boolean hasDependencies() { return !dependencies.isEmpty(); }

    /**
     * Returns true if ALL prerequisite tasks are in DONE state.
     */
    public boolean isDependenciesCompleted() {
        return dependencies.stream().allMatch(t -> t.getStatus() == TaskStatus.DONE);
    }

    // ── History ─────────────────────────────────────────────────────────────

    public List<TaskHistoryEntry> getHistory() { return history; }

    public void addHistoryEntry(TaskHistoryEntry entry) {
        if (entry != null) history.add(entry);
    }

    // ── Status Transitions ──────────────────────────────────────────────────

    /**
     * Updates the task's status after validating the transition.
     * Rules:
     *   - DONE is terminal; no transition away from DONE is allowed.
     *   - IN_PROGRESS requires all dependencies to be DONE.
     */
    public void setStatus(TaskStatus newStatus) throws InvalidTaskStateException {
        if (this.status == TaskStatus.DONE && newStatus != TaskStatus.DONE) {
            throw new InvalidTaskStateException(
                "Task [" + taskId + "] is already DONE and cannot transition to " + newStatus);
        }
        if (newStatus == TaskStatus.IN_PROGRESS && !isDependenciesCompleted()) {
            throw new InvalidTaskStateException(
                "Task [" + taskId + "] cannot move to IN_PROGRESS: not all dependencies are DONE.");
        }
        this.status = newStatus;
    }

    /** Convenience — marks the task DONE unconditionally (called by TaskManager after validations). */
    public void markAsDone(User user) throws InvalidTaskStateException {
        setStatus(TaskStatus.DONE);
        addHistoryEntry(new TaskHistoryEntry(taskId, "COMPLETED", user,
                "Task marked as DONE by " + (user != null ? user.getName() : "SYSTEM")));
    }

    /** Changes priority and logs the change. */
    public void changePriority(PriorityLevel newPriority, User user) {
        PriorityLevel old = this.priority;
        this.priority = newPriority;
        addHistoryEntry(new TaskHistoryEntry(taskId, "PRIORITY_CHANGE", user,
                "Priority changed from " + old + " to " + newPriority));
    }

    /** Updates description and logs the change. */
    public void updateDescription(String newDesc, User user) {
        this.description = newDesc;
        addHistoryEntry(new TaskHistoryEntry(taskId, "DESCRIPTION_UPDATE", user,
                "Description updated by " + (user != null ? user.getName() : "SYSTEM")));
    }

    // ── Comparable (for PriorityQueue: CRITICAL > HIGH > MEDIUM > LOW) ──────

    @Override
    public int compareTo(Task other) {
        // Reversed so that higher priority value = comes first in min-heap PriorityQueue
        return Integer.compare(other.priority.getValue(), this.priority.getValue());
    }

    // ── Display ─────────────────────────────────────────────────────────────

    public String displayTask() {
        StringBuilder sb = new StringBuilder();
        sb.append("╔══════════════════════════════════════════\n");
        sb.append(String.format("║ Task ID    : %s%n", taskId));
        sb.append(String.format("║ Title      : %s%n", title));
        sb.append(String.format("║ Status     : %s%n", status));
        sb.append(String.format("║ Priority   : %s%n", priority));
        sb.append(String.format("║ Category   : %s%n", category));
        sb.append(String.format("║ Deadline   : %s%n", deadline));
        sb.append(String.format("║ Assigned   : %s%n",
                assignedEngineer != null ? assignedEngineer.getName() : "Unassigned"));
        if (!dependencies.isEmpty()) {
            sb.append("║ Depends on : ");
            dependencies.forEach(d -> sb.append(d.getTaskId()).append(" "));
            sb.append("\n");
        }
        sb.append("╚══════════════════════════════════════════\n");
        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("Task[%s | %s | %s | %s]", taskId, title, status, priority);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Task)) return false;
        return taskId.equals(((Task) obj).taskId);
    }

    @Override
    public int hashCode() { return taskId.hashCode(); }
}
