package strms.model;

/**
 * Manager user — can assign tasks, monitor progress, and generate reports.
 * Cannot create or delete tasks.
 */
public class Manager extends User {

    public Manager(String userId, String name, String email) {
        super(userId, name, email);
    }

    @Override public boolean canCreateTask()     { return false; }
    @Override public boolean canDeleteTask()     { return false; }
    @Override public boolean canAssignTask()     { return true; }
    @Override public boolean canGenerateReport() { return true; }
    @Override public String  getRoleName()       { return "MANAGER"; }
}
