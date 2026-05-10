package manager;

import model.Task;
import model.TaskHistoryEntry;

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
}