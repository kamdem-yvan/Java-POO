package user;

public class Engineer extends User {

    public Engineer(String id, String n, String e) {
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
        return false;
    }

    @Override
    public boolean canGenerateReport() {
        return false;
    }

    @Override
    public String getRoleName() {
        return "Engineer";
    }
}