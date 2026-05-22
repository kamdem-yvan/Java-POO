package strms.model;

import java.time.LocalDateTime;

/**
 * Abstract superclass representing all user roles in STRMS.
 * Encapsulates common attributes and defines role-based permission contracts.
 */
public abstract class User {

    private final String userId;
    private String name;
    private String email;
    private final LocalDateTime registrationDate;

    public User(String userId, String name, String email) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.registrationDate = LocalDateTime.now();
    }

    // ── Getters & Setters ───────────────────────────────────────────────────

    public String getUserId()                    { return userId; }
    public String getName()                      { return name; }
    public void   setName(String name)           { this.name = name; }
    public String getEmail()                     { return email; }
    public void   setEmail(String email)         { this.email = email; }
    public LocalDateTime getRegistrationDate()   { return registrationDate; }

    // ── Abstract Permission Methods ─────────────────────────────────────────

    public abstract boolean canCreateTask();
    public abstract boolean canDeleteTask();
    public abstract boolean canAssignTask();
    public abstract boolean canGenerateReport();
    public abstract String  getRoleName();

    @Override
    public String toString() {
        return String.format("[%s] %s (%s) — %s", getRoleName(), name, userId, email);
    }
}
