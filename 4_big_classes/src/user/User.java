package user;

import java.time.LocalDateTime;

public abstract class User {
    protected String userId;
    protected String name;
    protected String email;
    protected LocalDateTime registrationDate;

    public User(String userId, String name, String email) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.registrationDate = LocalDateTime.now();
    }

    // Getters
    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public LocalDateTime getRegistrationDate() {
        return registrationDate;
    }

    // Permission methods
    public abstract boolean canCreateTask();

    public abstract boolean canDeleteTask();

    public abstract boolean canAssignTask();

    public abstract boolean canGenerateReport();

    // Role name
    public abstract String getRoleName();

    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", registrationDate=" + registrationDate +
                ", role=" + getRoleName() +
                '}';
    }
}




