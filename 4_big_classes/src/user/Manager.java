package user;

public class Manager extends User {

    public Manager(String id, String n, String e) {
        super(id, n, e);
    }

    @Override
    public boolean canCreateTask() {
        return false;
    }

    @Override
    public boolean canDeleteTask() {
        return false;
    }

    @Override
    public boolean canAssignTask() {
        return true;
    }

    @Override
    public boolean canGenerateReport() {
        return true;
    }

    @Override
    public String getRoleName() {
        return "Manager";
    }
}