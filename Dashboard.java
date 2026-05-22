package strms.model;

/**
 * Admin user — full privileges: create, delete, assign tasks and generate reports.
 */
public class Admin extends User {

    public Admin(String userId, String name, String email) {
        super(userId, name, email);
    }

    @Override public boolean canCreateTask()     { return true; }
    @Override public boolean canDeleteTask()     { return true; }
    @Override public boolean canAssignTask()     { return true; }
    @Override public boolean canGenerateReport() { return true; }
    @Override public String  getRoleName()       { return "ADMIN"; }
}
