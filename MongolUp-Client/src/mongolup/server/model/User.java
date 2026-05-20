package mongolup.server.model;

import java.io.Serializable;
import java.util.Date;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private int userId;
    private String username;
    private String email;
    private String fullName;
    private String avatarUrl;
    private String role;        // admin | manager | member
    private boolean isActive;
    private Date lastLogin;
    private Date createdAt;
    private String sessionToken; // set only on login response

    public User() {}

    public int getUserId()          { return userId; }
    public void setUserId(int v)    { userId = v; }

    public String getUsername()         { return username; }
    public void setUsername(String v)   { username = v; }

    public String getEmail()            { return email; }
    public void setEmail(String v)      { email = v; }

    public String getFullName()         { return fullName; }
    public void setFullName(String v)   { fullName = v; }

    public String getAvatarUrl()        { return avatarUrl; }
    public void setAvatarUrl(String v)  { avatarUrl = v; }

    public String getRole()             { return role; }
    public void setRole(String v)       { role = v; }

    public boolean isActive()           { return isActive; }
    public void setActive(boolean v)    { isActive = v; }

    public Date getLastLogin()          { return lastLogin; }
    public void setLastLogin(Date v)    { lastLogin = v; }

    public Date getCreatedAt()          { return createdAt; }
    public void setCreatedAt(Date v)    { createdAt = v; }

    public String getSessionToken()         { return sessionToken; }
    public void setSessionToken(String v)   { sessionToken = v; }

    /** Initials for avatar (e.g. "Эрдэнэтуяа Баяр" → "ЭБ") */
    public String getInitials() {
        if (fullName == null || fullName.isBlank()) return "?";
        String[] parts = fullName.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!p.isEmpty()) sb.appendCodePoint(p.codePointAt(0));
        }
        return sb.toString().toUpperCase();
    }

    @Override
    public String toString() { return fullName != null ? fullName : username; }
}
