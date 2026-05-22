package strms.manager;

import strms.enums.PriorityLevel;
import strms.enums.TaskStatus;
import strms.exceptions.*;
import strms.model.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Central controller of the STRMS.
 *
 * Responsibilities:
 *  - Manage the full lifecycle of tasks (add, update, delete, complete).
 *  - Enforce role-based permissions for every operation.
 *  - Maintain and validate the task dependency graph (circular-dependency detection via DFS).
 *  - Track which tasks are in-progress (HashSet) and schedule ready tasks (PriorityQueue).
 *  - Persist tasks, users and history to/from files via FileManager.
 *  - Provide query methods for reporting and the Dashboard.
 */
public class TaskManager {

    // ── Core data structures ────────────────────────────────────────────────
    private final HashMap<String, Task>                tasks;
    private final HashMap<String, User>                users;
    private final PriorityQueue<Task>                  readyQueue;
    private final HashSet<Task>                        inProgressTasks;
    private final HashMap<String, List<TaskHistoryEntry>> taskHistory;

    // ── Constructor ─────────────────────────────────────────────────────────
    public TaskManager() {
        tasks           = new HashMap<>();
        users           = new HashMap<>();
        readyQueue      = new PriorityQueue<>();   // Task.compareTo drives order
        inProgressTasks = new HashSet<>();
        taskHistory     = new HashMap<>();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // USER MANAGEMENT
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Registers a new user in the system.
     * @throws DuplicateUserException if the user ID already exists.
     */
    public void addUser(User user) throws DuplicateUserException {
        if (user == null) throw new IllegalArgumentException("User cannot be null.");
        if (users.containsKey(user.getUserId()))
            throw new DuplicateUserException("User with ID [" + user.getUserId() + "] already exists.");
        users.put(user.getUserId(), user);
    }

    /** Returns the user with the given ID or throws UserNotFoundException. */
    public User findUser(String userId) throws UserNotFoundException {
        User u = users.get(userId);
        if (u == null) throw new UserNotFoundException("User [" + userId + "] not found.");
        return u;
    }

    public List<User> getAllUsers() { return new ArrayList<>(users.values()); }

    /** Returns all users of a specific role class. */
    public <T extends User> List<T> getUsersByRole(Class<T> roleClass) {
        return users.values().stream()
                .filter(roleClass::isInstance)
                .map(roleClass::cast)
                .collect(Collectors.toList());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TASK LIFECYCLE
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Adds a new task to the system.
     * Only users with canCreateTask() == true are allowed.
     *
     * @throws InvalidRoleException   if the user lacks permission.
     * @throws DuplicateTaskException if a task with the same ID already exists.
     */
    public void addTask(Task task, User user)
            throws InvalidRoleException, DuplicateTaskException {
        if (!user.canCreateTask())
            throw new InvalidRoleException(user.getRoleName() + " [" + user.getName()
                    + "] is not allowed to create tasks.");
        if (tasks.containsKey(task.getTaskId()))
            throw new DuplicateTaskException("Task [" + task.getTaskId() + "] already exists.");

        tasks.put(task.getTaskId(), task);
        taskHistory.put(task.getTaskId(), task.getHistory());

        TaskHistoryEntry entry = new TaskHistoryEntry(
                task.getTaskId(), "CREATED", user,
                "Task created by " + user.getName() + " with priority " + task.getPriority());
        task.addHistoryEntry(entry);

        updateReadyQueue();
        System.out.println("[STRMS] Task [" + task.getTaskId() + "] created by " + user.getName());
    }

    /**
     * Deletes a task from the system.
     * Only users with canDeleteTask() == true are allowed.
     *
     * @throws InvalidRoleException  if the user lacks permission.
     * @throws TaskNotFoundException if the task does not exist.
     */
    public void deleteTask(String taskId, User user)
            throws InvalidRoleException, TaskNotFoundException {
        if (!user.canDeleteTask())
            throw new InvalidRoleException(user.getRoleName() + " cannot delete tasks.");
        Task task = findTask(taskId);

        // Remove this task from the dependencies of all other tasks
        for (Task t : tasks.values()) {
            t.removeDependency(task);
        }

        tasks.remove(taskId);
        taskHistory.remove(taskId);
        readyQueue.remove(task);
        inProgressTasks.remove(task);

        System.out.println("[STRMS] Task [" + taskId + "] deleted by " + user.getName());
    }

    /**
     * Assigns a task to an engineer.
     * Allowed for: Admin and Manager.
     * The task must exist and not already be assigned.
     * If all dependencies are DONE the task moves to IN_PROGRESS; otherwise stays BLOCKED.
     *
     * @throws InvalidRoleException           if the user cannot assign tasks.
     * @throws TaskNotFoundException          if task or engineer not found.
     * @throws UserNotFoundException          if engineer ID does not exist.
     * @throws DependencyNotCompletedException if dependencies prevent starting.
     * @throws InvalidTaskStateException      if the state transition is illegal.
     */
    public void assignTask(String taskId, String engineerId, User user)
            throws InvalidRoleException, TaskNotFoundException, UserNotFoundException,
                   DependencyNotCompletedException, InvalidTaskStateException {
        if (!user.canAssignTask())
            throw new InvalidRoleException(user.getRoleName() + " cannot assign tasks.");

        Task task = findTask(taskId);
        User engineerUser = findUser(engineerId);
        if (!(engineerUser instanceof Engineer))
            throw new InvalidRoleException("User [" + engineerId + "] is not an Engineer.");
        Engineer engineer = (Engineer) engineerUser;

        task.setAssignedEngineer(engineer);

        TaskHistoryEntry entry = new TaskHistoryEntry(taskId, "ASSIGNED", user,
                "Task assigned to engineer " + engineer.getName() + " by " + user.getName());
        task.addHistoryEntry(entry);

        // Determine status based on dependencies
        if (task.isDependenciesCompleted()) {
            task.setStatus(TaskStatus.IN_PROGRESS);
            inProgressTasks.add(task);
            readyQueue.remove(task);
            task.addHistoryEntry(new TaskHistoryEntry(taskId, "STATUS_CHANGE", user,
                    "Status changed to IN_PROGRESS (all dependencies satisfied)"));
        } else {
            task.setStatus(TaskStatus.BLOCKED);
            task.addHistoryEntry(new TaskHistoryEntry(taskId, "STATUS_CHANGE", user,
                    "Status changed to BLOCKED (pending dependencies)"));
        }

        System.out.println("[STRMS] Task [" + taskId + "] assigned to " + engineer.getName());
    }

    /**
     * Marks a task as DONE.
     * Only the assigned engineer can complete a task.
     * After completion, dependent tasks whose prerequisites are now all DONE are automatically unblocked.
     *
     * @throws TaskNotFoundException          if task not found.
     * @throws InvalidRoleException           if user is not the assigned engineer.
     * @throws DependencyNotCompletedException if dependencies are not yet DONE.
     * @throws InvalidTaskStateException      if the transition is invalid.
     */
    public void completeTask(String taskId, User user)
            throws TaskNotFoundException, InvalidRoleException,
                   DependencyNotCompletedException, InvalidTaskStateException {
        Task task = findTask(taskId);

        // Only the assigned engineer can complete a task
        if (!(user instanceof Engineer) || task.getAssignedEngineer() == null
                || !task.getAssignedEngineer().getUserId().equals(user.getUserId())) {
            throw new InvalidRoleException(
                "Only the assigned engineer can complete task [" + taskId + "].");
        }
        if (!task.isDependenciesCompleted()) {
            throw new DependencyNotCompletedException(
                "Cannot complete task [" + taskId + "]: not all dependencies are DONE.");
        }

        task.markAsDone(user);
        inProgressTasks.remove(task);

        System.out.println("[STRMS] Task [" + taskId + "] completed by " + user.getName());

        // Unblock dependent tasks
        activateDependents(task);
        updateReadyQueue();
    }

    /**
     * Starts a task (TODO/BLOCKED → IN_PROGRESS).
     * Only the assigned engineer can start a task.
     */
    public void startTask(String taskId, User user)
            throws TaskNotFoundException, InvalidRoleException,
                   DependencyNotCompletedException, InvalidTaskStateException {
        Task task = findTask(taskId);

        if (!(user instanceof Engineer) || task.getAssignedEngineer() == null
                || !task.getAssignedEngineer().getUserId().equals(user.getUserId())) {
            throw new InvalidRoleException("Only the assigned engineer can start task [" + taskId + "].");
        }
        if (!task.isDependenciesCompleted()) {
            // Log the failed attempt
            task.addHistoryEntry(new TaskHistoryEntry(taskId, "START_FAILED", user,
                    "Attempt to start blocked by unresolved dependencies: " + getBlockingDeps(task)));
            throw new DependencyNotCompletedException(
                "Task [" + taskId + "] cannot start. Incomplete dependencies: " + getBlockingDeps(task));
        }

        task.setStatus(TaskStatus.IN_PROGRESS);
        inProgressTasks.add(task);
        readyQueue.remove(task);
        task.addHistoryEntry(new TaskHistoryEntry(taskId, "STATUS_CHANGE", user,
                "Status changed to IN_PROGRESS by " + user.getName()));
        System.out.println("[STRMS] Task [" + taskId + "] started by " + user.getName());
    }

    /**
     * Updates mutable fields of an existing task.
     * Only users with canCreateTask() (Admin) may update arbitrary fields.
     */
    public void updateTask(Task updatedTask, User user)
            throws InvalidRoleException, TaskNotFoundException, InvalidTaskStateException {
        if (!user.canCreateTask())
            throw new InvalidRoleException(user.getRoleName() + " cannot update tasks.");
        Task existing = findTask(updatedTask.getTaskId());

        existing.setTitle(updatedTask.getTitle());
        existing.setDescription(updatedTask.getDescription());
        existing.setPriority(updatedTask.getPriority());
        existing.setCategory(updatedTask.getCategory());
        existing.setDeadline(updatedTask.getDeadline());

        existing.addHistoryEntry(new TaskHistoryEntry(updatedTask.getTaskId(), "UPDATED", user,
                "Task updated by " + user.getName()));
        updateReadyQueue();
        System.out.println("[STRMS] Task [" + updatedTask.getTaskId() + "] updated.");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DEPENDENCY MANAGEMENT
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Adds a dependency: {@code taskId} depends on {@code dependsOnId}.
     * Validates existence and checks for circular dependencies before adding.
     *
     * @throws TaskNotFoundException        if either task is not found.
     * @throws CircularDependencyException  if adding would create a cycle.
     */
    public void addDependency(String taskId, String dependsOnId, User user)
            throws TaskNotFoundException, CircularDependencyException, InvalidRoleException {
        if (!user.canCreateTask())
            throw new InvalidRoleException(user.getRoleName() + " cannot manage dependencies.");
        Task task      = findTask(taskId);
        Task dependsOn = findTask(dependsOnId);

        if (task.equals(dependsOn))
            throw new CircularDependencyException("A task cannot depend on itself: [" + taskId + "].");

        // Would adding dependsOn → task create a cycle in the existing graph?
        if (detectCircularDependency(dependsOn, task))
            throw new CircularDependencyException(
                "Adding dependency [" + taskId + "] → [" + dependsOnId
                + "] would create a circular dependency.");

        task.addDependency(dependsOn);
        updateTaskStatusBasedOnDependencies(task);

        task.addHistoryEntry(new TaskHistoryEntry(taskId, "DEPENDENCY_ADDED", user,
                "Dependency added: [" + taskId + "] now depends on [" + dependsOnId + "]"));
        System.out.println("[STRMS] Dependency added: [" + taskId + "] depends on [" + dependsOnId + "]");
    }

    /**
     * Removes a dependency between two tasks and re-evaluates the task's status.
     */
    public void removeDependency(String taskId, String dependsOnId, User user)
            throws TaskNotFoundException, InvalidRoleException {
        if (!user.canCreateTask())
            throw new InvalidRoleException(user.getRoleName() + " cannot manage dependencies.");
        Task task      = findTask(taskId);
        Task dependsOn = findTask(dependsOnId);

        boolean removed = task.removeDependency(dependsOn);
        if (removed) {
            updateTaskStatusBasedOnDependencies(task);
            task.addHistoryEntry(new TaskHistoryEntry(taskId, "DEPENDENCY_REMOVED", user,
                    "Dependency removed: [" + taskId + "] no longer depends on [" + dependsOnId + "]"));
        }
    }

    /** Returns the prerequisite tasks of a given task. */
    public List<Task> getDependencies(String taskId) throws TaskNotFoundException {
        return findTask(taskId).getDependencies();
    }

    /** Returns all tasks that directly depend on the given task. */
    public List<Task> getDependents(String taskId) throws TaskNotFoundException {
        Task target = findTask(taskId);
        return tasks.values().stream()
                .filter(t -> t.getDependencies().contains(target))
                .collect(Collectors.toList());
    }

    /**
     * Detects whether there is a path from {@code startTask} to {@code targetTask}
     * in the existing dependency graph (i.e., would adding startTask → targetTask create a cycle?).
     *
     * Uses iterative DFS.
     */
    public boolean detectCircularDependency(Task startTask, Task targetTask) {
        return dfsCycleDetection(startTask, targetTask, new HashSet<>());
    }

    /**
     * Recursive DFS: returns true if {@code target} is reachable from {@code current}
     * through the existing dependency edges.
     */
    public boolean dfsCycleDetection(Task current, Task target, Set<String> visited) {
        if (current.equals(target)) return true;
        if (visited.contains(current.getTaskId())) return false;
        visited.add(current.getTaskId());
        for (Task dep : current.getDependencies()) {
            if (dfsCycleDetection(dep, target, visited)) return true;
        }
        return false;
    }

    /**
     * Re-evaluates a task's status after its dependency list changes:
     *   - All deps DONE  → TODO (or keep IN_PROGRESS if already started)
     *   - Any dep not DONE and task has an engineer → BLOCKED
     */
    public void updateTaskStatusBasedOnDependencies(Task task) {
        if (task.getStatus() == TaskStatus.DONE) return;
        if (task.getStatus() == TaskStatus.IN_PROGRESS) return;

        if (task.isDependenciesCompleted()) {
            if (task.getStatus() == TaskStatus.BLOCKED) {
                try { task.setStatus(TaskStatus.TODO); }
                catch (Exception ignored) { /* won't happen here */ }
            }
        } else {
            try { task.setStatus(TaskStatus.BLOCKED); }
            catch (Exception ignored) { }
        }
    }

    /** After a task is completed, activate (unblock) tasks that depended only on it. */
    private void activateDependents(Task completedTask) {
        for (Task t : tasks.values()) {
            if (t.getDependencies().contains(completedTask) && t.getStatus() == TaskStatus.BLOCKED) {
                if (t.isDependenciesCompleted()) {
                    try { t.setStatus(TaskStatus.TODO); } catch (Exception ignored) {}
                    t.addHistoryEntry(new TaskHistoryEntry(t.getTaskId(), "UNBLOCKED", null,
                            "Unblocked because dependency [" + completedTask.getTaskId() + "] is now DONE."));
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PRIORITY QUEUE & IN-PROGRESS TRACKING
    // ══════════════════════════════════════════════════════════════════════════

    /** Rebuilds the ready queue from tasks that are TODO and have no pending deps. */
    public void updateReadyQueue() {
        readyQueue.clear();
        for (Task t : tasks.values()) {
            if (t.getStatus() == TaskStatus.TODO && t.isDependenciesCompleted()) {
                readyQueue.offer(t);
            }
        }
    }

    /** Returns (and removes) the highest-priority ready task. */
    public Task getNextReadyTask() { return readyQueue.poll(); }

    /** Peeks at the highest-priority ready task without removing it. */
    public Task peekNextReadyTask() { return readyQueue.peek(); }

    /** Processes (starts) the next highest-priority ready task in the queue. */
    public void processNextTask() {
        Task next = getNextReadyTask();
        if (next == null) {
            System.out.println("[STRMS] No ready tasks available.");
            return;
        }
        System.out.println("[STRMS] Processing next task: " + next);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // QUERY & SEARCH
    // ══════════════════════════════════════════════════════════════════════════

    /** Finds a task by ID. @throws TaskNotFoundException if absent. */
    public Task findTask(String taskId) throws TaskNotFoundException {
        Task t = tasks.get(taskId);
        if (t == null) throw new TaskNotFoundException("Task [" + taskId + "] not found.");
        return t;
    }

    public List<Task> getAllTasks() { return new ArrayList<>(tasks.values()); }

    public List<Task> getTasksByStatus(TaskStatus status) {
        return tasks.values().stream()
                .filter(t -> t.getStatus() == status)
                .collect(Collectors.toList());
    }

    public List<Task> getTasksByPriority(PriorityLevel priority) {
        return tasks.values().stream()
                .filter(t -> t.getPriority() == priority)
                .sorted()
                .collect(Collectors.toList());
    }

    public List<Task> getTasksByUser(String userId) {
        return tasks.values().stream()
                .filter(t -> t.getAssignedEngineer() != null
                        && t.getAssignedEngineer().getUserId().equals(userId))
                .collect(Collectors.toList());
    }

    public List<Task> getOverdueTasks() {
        java.time.LocalDate today = java.time.LocalDate.now();
        return tasks.values().stream()
                .filter(t -> t.getDeadline() != null
                        && t.getDeadline().isBefore(today)
                        && t.getStatus() != TaskStatus.DONE)
                .collect(Collectors.toList());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PRINTING
    // ══════════════════════════════════════════════════════════════════════════

    public void printInProgressTasks() {
        System.out.println("=== IN-PROGRESS TASKS ===");
        inProgressTasks.forEach(t -> System.out.println(t.displayTask()));
    }

    public void printAllTasks() {
        System.out.println("=== ALL TASKS ===");
        tasks.values().forEach(t -> System.out.println(t.displayTask()));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PERSISTENCE
    // ══════════════════════════════════════════════════════════════════════════

    public void saveTasksToFile(String filename) throws strms.exceptions.FilePersistenceException {
        strms.utility.FileManager.saveTasks(tasks, filename);
    }

    public void loadTasksFromFile(String filename) throws strms.exceptions.FilePersistenceException {
        Map<String, Task> loaded = strms.utility.FileManager.loadTasks(filename);
        tasks.putAll(loaded);
        loaded.forEach((id, t) -> taskHistory.put(id, t.getHistory()));
        updateReadyQueue();
    }

    public void saveUsersToFile(String filename) throws strms.exceptions.FilePersistenceException {
        strms.utility.FileManager.saveUsers(users, filename);
    }

    public void loadUsersFromFile(String filename) throws strms.exceptions.FilePersistenceException {
        users.putAll(strms.utility.FileManager.loadUsers(filename));
    }

    public void saveHistoryToFile(String filename) throws strms.exceptions.FilePersistenceException {
        strms.utility.FileManager.saveHistory(taskHistory, filename);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // INTERNAL HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    /** Returns a comma-separated list of IDs of unfinished dependencies. */
    private String getBlockingDeps(Task task) {
        return task.getDependencies().stream()
                .filter(d -> d.getStatus() != TaskStatus.DONE)
                .map(Task::getTaskId)
                .collect(Collectors.joining(", "));
    }

    // ── Accessors for testing & utilities ──────────────────────────────────

    public HashMap<String, Task>                         getTasks()       { return tasks; }
    public HashMap<String, User>                         getUsers()       { return users; }
    public PriorityQueue<Task>                           getReadyQueue()  { return readyQueue; }
    public HashSet<Task>                                 getInProgress()  { return inProgressTasks; }
    public HashMap<String, List<TaskHistoryEntry>>       getTaskHistory() { return taskHistory; }
}
