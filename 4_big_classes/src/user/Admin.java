package user;

public class Admin extends User {

    public Admin(String id, String n, String e) {
        super(id, n, e);
    }

    @Override
    public boolean canCreateTask() {
        return true;
    }

    @Override
    public boolean canDeleteTask() {
        return true;
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
        return "Admin";
    }
}