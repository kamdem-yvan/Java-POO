package strms;

import org.junit.jupiter.api.*;
import strms.enums.PriorityLevel;
import strms.enums.TaskCategory;
import strms.enums.TaskStatus;
import strms.exceptions.*;
import strms.manager.TaskManager;
import strms.model.*;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive JUnit 5 test suite for the STRMS.
 *
 * Covers:
 *  - Valid dependency addition
 *  - Circular dependency detection & rejection
 *  - Graph integrity after rejected dependency
 *  - Exception handling
 *  - Dependency removal
 *  - Task state transitions
 *  - Role-based access control
 *  - Task lifecycle (add, assign, start, complete, delete)
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TaskManagerTest {

    // ── Fixtures shared across tests ──────────────────────────────────────────

    private TaskManager tm;
    private Admin    alice;
    private Manager  bob;
    private Engineer charlie;
    private Task     taskA, taskB, taskC, taskD;

    @BeforeEach
    void setUp() throws Exception {
        tm      = new TaskManager();
        alice   = new Admin("U001", "Alice",   "alice@test.io");
        bob     = new Manager("U002", "Bob",   "bob@test.io");
        charlie = new Engineer("U003", "Charlie", "charlie@test.io");

        tm.addUser(alice);
        tm.addUser(bob);
        tm.addUser(charlie);

        taskA = new Task("T-A", "Task A", "Desc A", PriorityLevel.HIGH,   TaskCategory.FEATURE, LocalDate.now().plusDays(5));
        taskB = new Task("T-B", "Task B", "Desc B", PriorityLevel.MEDIUM, TaskCategory.FEATURE, LocalDate.now().plusDays(6));
        taskC = new Task("T-C", "Task C", "Desc C", PriorityLevel.LOW,    TaskCategory.BUGFIX,  LocalDate.now().plusDays(7));
        taskD = new Task("T-D", "Task D", "Desc D", PriorityLevel.CRITICAL, TaskCategory.RESEARCH, LocalDate.now().plusDays(2));

        tm.addTask(taskA, alice);
        tm.addTask(taskB, alice);
        tm.addTask(taskC, alice);
        tm.addTask(taskD, alice);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 1. VALID DEPENDENCY ADDITION
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(1)
    @DisplayName("Valid linear dependency: A → B → C")
    void testAddValidDependencies() throws Exception {
        tm.addDependency("T-B", "T-A", alice);   // B depends on A
        tm.addDependency("T-C", "T-B", alice);   // C depends on B

        assertTrue(taskB.getDependencies().contains(taskA));
        assertTrue(taskC.getDependencies().contains(taskB));
        assertFalse(taskA.hasDependencies());     // A has no prerequisites
    }

    @Test
    @Order(2)
    @DisplayName("Task with pending dependency is BLOCKED")
    void testTaskIsBlockedWhenDependencyPending() throws Exception {
        tm.addDependency("T-B", "T-A", alice);
        assertEquals(TaskStatus.BLOCKED, taskB.getStatus());
    }

    @Test
    @Order(3)
    @DisplayName("Task becomes TODO when dependency is DONE")
    void testTaskUnblockedWhenDependencyCompleted() throws Exception {
        tm.addDependency("T-B", "T-A", alice);

        // Complete task A
        tm.assignTask("T-A", "U003", bob);
        tm.startTask("T-A", charlie);
        tm.completeTask("T-A", charlie);

        assertEquals(TaskStatus.DONE, taskA.getStatus());
        // B should be unblocked
        assertNotEquals(TaskStatus.BLOCKED, taskB.getStatus());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 2. CIRCULAR DEPENDENCY DETECTION & REJECTION
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(4)
    @DisplayName("Direct circular dependency (A → B, B → A) is rejected")
    void testDirectCircularDependencyRejected() throws Exception {
        tm.addDependency("T-B", "T-A", alice);   // B depends on A

        assertThrows(CircularDependencyException.class,
                () -> tm.addDependency("T-A", "T-B", alice),  // A depends on B → cycle
                "Should throw CircularDependencyException");
    }

    @Test
    @Order(5)
    @DisplayName("Indirect circular dependency (A→B→C, C→A) is rejected")
    void testIndirectCircularDependencyRejected() throws Exception {
        tm.addDependency("T-B", "T-A", alice);   // B → A
        tm.addDependency("T-C", "T-B", alice);   // C → B  (chain: C→B→A)

        CircularDependencyException ex = assertThrows(CircularDependencyException.class,
                () -> tm.addDependency("T-A", "T-C", alice),  // A → C → cycle
                "Should throw CircularDependencyException for indirect cycle");
        assertNotNull(ex.getMessage());
        assertFalse(ex.getMessage().isBlank());
    }

    @Test
    @Order(6)
    @DisplayName("Self-dependency is rejected")
    void testSelfDependencyRejected() {
        assertThrows(CircularDependencyException.class,
                () -> tm.addDependency("T-A", "T-A", alice));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 3. GRAPH INTEGRITY AFTER REJECTION
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(7)
    @DisplayName("Graph remains unchanged after rejected circular dependency")
    void testGraphIntegrityAfterRejection() throws Exception {
        tm.addDependency("T-B", "T-A", alice);
        tm.addDependency("T-C", "T-B", alice);

        int depsBeforeA = taskA.getDependencies().size(); // 0
        int depsBeforeB = taskB.getDependencies().size(); // 1
        int depsBeforeC = taskC.getDependencies().size(); // 1

        try {
            tm.addDependency("T-A", "T-C", alice);  // would create cycle
        } catch (CircularDependencyException ignored) {}

        assertEquals(depsBeforeA, taskA.getDependencies().size(), "A's deps must be unchanged");
        assertEquals(depsBeforeB, taskB.getDependencies().size(), "B's deps must be unchanged");
        assertEquals(depsBeforeC, taskC.getDependencies().size(), "C's deps must be unchanged");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 4. DEPENDENCY REMOVAL
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(8)
    @DisplayName("Removing a dependency unblocks the task")
    void testRemoveDependencyUnblocksTask() throws Exception {
        tm.addDependency("T-B", "T-A", alice);
        assertEquals(TaskStatus.BLOCKED, taskB.getStatus());

        tm.removeDependency("T-B", "T-A", alice);
        assertFalse(taskB.getDependencies().contains(taskA));
        assertNotEquals(TaskStatus.BLOCKED, taskB.getStatus());
    }

    @Test
    @Order(9)
    @DisplayName("Removed dependency is no longer in dependency list")
    void testRemovedDependencyNotPresent() throws Exception {
        tm.addDependency("T-C", "T-A", alice);
        tm.addDependency("T-C", "T-B", alice);

        tm.removeDependency("T-C", "T-A", alice);

        assertFalse(taskC.getDependencies().contains(taskA));
        assertTrue(taskC.getDependencies().contains(taskB));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 5. TASK STATE TRANSITIONS
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(10)
    @DisplayName("Task cannot transition from DONE back to IN_PROGRESS")
    void testDoneIsTerminalState() throws Exception {
        tm.assignTask("T-A", "U003", bob);
        tm.startTask("T-A", charlie);
        tm.completeTask("T-A", charlie);

        assertEquals(TaskStatus.DONE, taskA.getStatus());
        assertThrows(InvalidTaskStateException.class,
                () -> taskA.setStatus(TaskStatus.IN_PROGRESS));
    }

    @Test
    @Order(11)
    @DisplayName("Cannot start task with unresolved dependencies")
    void testCannotStartWithUnresolvedDeps() throws Exception {
        tm.addDependency("T-B", "T-A", alice);
        tm.assignTask("T-B", "U003", bob);  // B is BLOCKED

        assertThrows(DependencyNotCompletedException.class,
                () -> tm.startTask("T-B", charlie));
    }

    @Test
    @Order(12)
    @DisplayName("Task initial status is TODO")
    void testInitialStatusIsTodo() {
        assertEquals(TaskStatus.TODO, taskD.getStatus());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 6. ROLE-BASED ACCESS CONTROL
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(13)
    @DisplayName("Engineer cannot create a task")
    void testEngineerCannotCreateTask() {
        Task newTask = new Task("T-E", "Unauthorized Task", "Desc",
                PriorityLevel.LOW, TaskCategory.BUGFIX, LocalDate.now().plusDays(1));
        assertThrows(InvalidRoleException.class, () -> tm.addTask(newTask, charlie));
    }

    @Test
    @Order(14)
    @DisplayName("Manager cannot delete a task")
    void testManagerCannotDeleteTask() {
        assertThrows(InvalidRoleException.class, () -> tm.deleteTask("T-A", bob));
    }

    @Test
    @Order(15)
    @DisplayName("Engineer cannot assign a task")
    void testEngineerCannotAssignTask() {
        assertThrows(InvalidRoleException.class,
                () -> tm.assignTask("T-A", "U003", charlie));
    }

    @Test
    @Order(16)
    @DisplayName("Non-assigned engineer cannot complete a task")
    void testNonAssignedEngineerCannotComplete() throws Exception {
        // Add a second engineer
        Engineer dave = new Engineer("U004", "Dave", "dave@test.io");
        tm.addUser(dave);

        tm.assignTask("T-A", "U003", bob);  // assigned to charlie
        assertThrows(InvalidRoleException.class, () -> tm.completeTask("T-A", dave));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 7. TASK NOT FOUND
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(17)
    @DisplayName("Finding a non-existent task throws TaskNotFoundException")
    void testFindNonExistentTask() {
        assertThrows(TaskNotFoundException.class, () -> tm.findTask("GHOST"));
    }

    @Test
    @Order(18)
    @DisplayName("Adding dependency with unknown task throws TaskNotFoundException")
    void testAddDependencyUnknownTask() {
        assertThrows(TaskNotFoundException.class,
                () -> tm.addDependency("T-A", "GHOST", alice));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 8. DUPLICATE PREVENTION
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(19)
    @DisplayName("Adding a task with duplicate ID throws DuplicateTaskException")
    void testDuplicateTaskRejected() {
        Task dup = new Task("T-A", "Duplicate", "Desc",
                PriorityLevel.LOW, TaskCategory.BUGFIX, LocalDate.now().plusDays(1));
        assertThrows(DuplicateTaskException.class, () -> tm.addTask(dup, alice));
    }

    @Test
    @Order(20)
    @DisplayName("Adding a user with duplicate ID throws DuplicateUserException")
    void testDuplicateUserRejected() {
        Admin dup = new Admin("U001", "Duplicate Alice", "dup@test.io");
        assertThrows(DuplicateUserException.class, () -> tm.addUser(dup));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 9. PRIORITY QUEUE
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(21)
    @DisplayName("Ready queue returns highest priority task first")
    void testPriorityQueueOrder() {
        tm.updateReadyQueue();
        Task next = tm.peekNextReadyTask();
        // taskD has CRITICAL priority — should be first
        assertNotNull(next);
        assertEquals(PriorityLevel.CRITICAL, next.getPriority());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 10. TASK HISTORY
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(22)
    @DisplayName("Task history is recorded on creation")
    void testHistoryRecordedOnCreate() {
        assertFalse(taskA.getHistory().isEmpty());
        assertEquals("CREATED", taskA.getHistory().get(0).getAction());
    }

    @Test
    @Order(23)
    @DisplayName("Task history is recorded on assignment")
    void testHistoryRecordedOnAssign() throws Exception {
        int before = taskA.getHistory().size();
        tm.assignTask("T-A", "U003", bob);
        assertTrue(taskA.getHistory().size() > before);
    }

    @Test
    @Order(24)
    @DisplayName("Failed start attempt is logged in task history")
    void testFailedStartLoggedInHistory() throws Exception {
        tm.addDependency("T-B", "T-A", alice);
        tm.assignTask("T-B", "U003", bob);

        try {
            tm.startTask("T-B", charlie);
        } catch (DependencyNotCompletedException ignored) {}

        boolean logged = taskB.getHistory().stream()
                .anyMatch(e -> e.getAction().equals("START_FAILED"));
        assertTrue(logged, "Failed start attempt should be logged in history");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 11. FULL LIFECYCLE SCENARIO
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(25)
    @DisplayName("Full task lifecycle: create → assign → start → complete")
    void testFullLifecycle() throws Exception {
        assertEquals(TaskStatus.TODO, taskA.getStatus());

        tm.assignTask("T-A", "U003", bob);
        assertEquals(TaskStatus.IN_PROGRESS, taskA.getStatus()); // no deps → directly IN_PROGRESS

        tm.startTask("T-A", charlie);  // Already IN_PROGRESS — should stay

        tm.completeTask("T-A", charlie);
        assertEquals(TaskStatus.DONE, taskA.getStatus());
    }

    @Test
    @Order(26)
    @DisplayName("Cascading unblock: completing A unblocks B which unblocks C")
    void testCascadingUnblock() throws Exception {
        tm.addDependency("T-B", "T-A", alice);
        tm.addDependency("T-C", "T-B", alice);

        assertEquals(TaskStatus.BLOCKED, taskB.getStatus());
        assertEquals(TaskStatus.BLOCKED, taskC.getStatus());

        // Complete A
        tm.assignTask("T-A", "U003", bob);
        tm.startTask("T-A", charlie);
        tm.completeTask("T-A", charlie);

        // B should be unblocked (deps satisfied); C still depends on B (not done)
        assertNotEquals(TaskStatus.BLOCKED, taskB.getStatus());
        assertEquals(TaskStatus.BLOCKED, taskC.getStatus());
    }
}
