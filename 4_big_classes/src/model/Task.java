package model;

import enums.PriorityLevel;
import enums.TaskCategory;
import enums.TaskStatus;

import user.Engineer;
import user.User;

import java.time.LocalDate;
import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.List;

public class Task implements Comparable<Task> {

    private String taskId;
    private String title;
    private String description;

    private PriorityLevel priority;
    private TaskStatus status;
    private TaskCategory category;

    private LocalDate deadline;
    private LocalDateTime creationDate;

    private List<Task> dependencies;
    private List<TaskHistoryEntry> history;

    private Engineer assignedEngineer;

    public Task(String taskId,
                String title,
                String description,
                PriorityLevel priority,
                TaskCategory category,
                LocalDate deadline) {

        this.taskId = taskId;
        this.title = title;
        this.description = description;

        this.priority = priority;
        this.category = category;

        this.deadline = deadline;

        this.creationDate = LocalDateTime.now();

        this.status = TaskStatus.TODO;

        this.dependencies = new ArrayList<>();
        this.history = new ArrayList<>();
    }

    // =========================
    // Getters & Setters
    // =========================

    public String getTaskId() {
        return taskId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public PriorityLevel getPriority() {
        return priority;
    }

    public void setPriority(PriorityLevel priority) {
        this.priority = priority;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public TaskCategory getCategory() {
        return category;
    }

    public void setCategory(TaskCategory category) {
        this.category = category;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public List<Task> getDependencies() {
        return dependencies;
    }

    public Engineer getAssignedEngineer() {
        return assignedEngineer;
    }

    public void setAssignedEngineer(Engineer engineer) {
        this.assignedEngineer = engineer;
    }

    public List<TaskHistoryEntry> getHistory() {
        return history;
    }

    // =========================
    // Dependency Methods
    // =========================

    public boolean addDependency(Task task) {

        if (task == null || dependencies.contains(task)) {
            return false;
        }

        dependencies.add(task);

        return true;
    }

    public boolean removeDependency(Task task) {
        return dependencies.remove(task);
    }

    public boolean hasDependencies() {
        return !dependencies.isEmpty();
    }

    public boolean isDependenciesCompleted() {

        for (Task task : dependencies) {

            if (task.getStatus() != TaskStatus.DONE) {
                return false;
            }
        }

        return true;
    }

    // =========================
    // History Methods
    // =========================

    public void addHistoryEntry(TaskHistoryEntry entry) {
        history.add(entry);
    }

    // =========================
    // Status Methods
    // =========================

    public void updateStatus(TaskStatus newStatus, User user) {

        this.status = newStatus;

        TaskHistoryEntry entry =
                new TaskHistoryEntry(
                        this.taskId,
                        "STATUS_UPDATE",
                        user,
                        "Task status changed to " + newStatus
                );

        addHistoryEntry(entry);
    }

    public void markAsDone(User user) {
        updateStatus(TaskStatus.DONE, user);
    }

    // =========================
    // Comparable
    // =========================

    @Override
    public int compareTo(Task other) {

        return other.priority.ordinal() - this.priority.ordinal();
    }

    // =========================
    // Display
    // =========================

    public String displayTask() {
        return this.toString();
    }

    @Override
    public String toString() {

        return "Task{" +
                "taskId='" + taskId + '\'' +
                ", title='" + title + '\'' +
                ", priority=" + priority +
                ", status=" + status +
                ", category=" + category +
                ", deadline=" + deadline +
                '}';
    }
}