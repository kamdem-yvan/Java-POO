package manager;

import model.Task;
import model.TaskHistoryEntry;

import user.Engineer;
import user.User;

import enums.PriorityLevel;
import enums.TaskStatus;

import java.util.*;

public class TaskManager {

    private HashMap<String, Task> tasks;

    private HashMap<String, User> users;

    private PriorityQueue<Task> readyQueue;

    private HashSet<Task> inProgressTasks;

    private HashMap<String, List<TaskHistoryEntry>> taskHistory;

    public TaskManager() {

        tasks = new HashMap<>();

        users = new HashMap<>();

        readyQueue = new PriorityQueue<>();

        inProgressTasks = new HashSet<>();

        taskHistory = new HashMap<>();
    }

    // =========================
    // User Management
    // =========================

    public void addUser(User user) {

        users.put(user.getUserId(), user);
    }

    public User findUser(String userId) {

        return users.get(userId);
    }

    public List<User> getAllUsers() {

        return new ArrayList<>(users.values());
    }

    // =========================
    // Task Management
    // =========================

    public void addTask(Task task, User user) {

        if (!user.canCreateTask()) {

            System.out.println("User does not have permission.");

            return;
        }

        tasks.put(task.getTaskId(), task);

        readyQueue.add(task);

        taskHistory.put(task.getTaskId(), task.getHistory());
    }

    public void deleteTask(String taskId, User user) {

        if (!user.canDeleteTask()) {

            System.out.println("Permission denied.");

            return;
        }

        Task removedTask = tasks.remove(taskId);

        if (removedTask != null) {

            readyQueue.remove(removedTask);

            inProgressTasks.remove(removedTask);

            taskHistory.remove(taskId);
        }
    }

    public List<Task> getAllTasks() {

        return new ArrayList<>(tasks.values());
    }

    public void printAllTasks() {

        for (Task task : tasks.values()) {

            System.out.println(task);
        }
    }

    public Task getNextReadyTask() {

        return readyQueue.peek();
    }

    public void assignTask(String taskId,
                           String engineerId,
                           User user) {

        if (!user.canAssignTask()) {

            System.out.println("Permission denied.");

            return;
        }

        Task task = tasks.get(taskId);

        User assignedUser = users.get(engineerId);

        if (task == null || assignedUser == null) {

            System.out.println("Task or engineer not found.");

            return;
        }

        if (!(assignedUser instanceof Engineer)) {

            System.out.println("Assigned user is not an engineer.");

            return;
        }

        task.setAssignedEngineer((Engineer) assignedUser);

        System.out.println("Task assigned successfully.");
    }

    public void startTask(String taskId, User user) {

        Task task = tasks.get(taskId);

        if (task == null) {

            System.out.println("Task not found.");

            return;
        }

        if (task.getAssignedEngineer() == null) {

            System.out.println("No engineer assigned.");

            return;
        }

        if (!task.isDependenciesCompleted()) {

            task.setStatus(TaskStatus.BLOCKED);

            System.out.println("Dependencies are not completed.");

            return;
        }

        task.updateStatus(TaskStatus.IN_PROGRESS, user);

        inProgressTasks.add(task);

        readyQueue.remove(task);

        System.out.println("Task started.");
    }
    public void completeTask(String taskId, User user) {

        Task task = tasks.get(taskId);

        if (task == null) {

            System.out.println("Task not found.");

            return;
        }

        if (task.getStatus() != TaskStatus.IN_PROGRESS) {

            System.out.println("Task is not in progress.");

            return;
        }

        task.markAsDone(user);

        inProgressTasks.remove(task);

        System.out.println("Task completed.");
    }
}