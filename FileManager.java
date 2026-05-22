package strms.model;

/**
 * Engineer user — can only work on and complete tasks assigned to them.
 */
public class Engineer extends User {

    public Engineer(String userId, String name, String email) {
        super(userId, name, email);
    }

    @Override public boolean canCreateTask()     { return false; }
    @Override public boolean canDeleteTask()     { return false; }
    @Override public boolean canAssignTask()     { return false; }
    @Override public boolean canGenerateReport() { return false; }
    @Override public String  getRoleName()       { return "ENGINEER"; }
}
