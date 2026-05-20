package mongolup.server.dao;

import mongolup.server.db.DatabaseConnection;
import mongolup.server.model.User;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UserDAO {

    public User login(String username, String password) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ? AND is_active = TRUE";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                String hash = rs.getString("password_hash");
                if (!BCrypt.checkpw(password, hash)) return null;
                User u = mapUser(rs);
                // create session
                String token = UUID.randomUUID().toString();
                insertSession(c, u.getUserId(), token);
                u.setSessionToken(token);
                updateLastLogin(c, u.getUserId());
                return u;
            }
        }
    }

    public User register(String username, String email, String password, String fullName)
            throws SQLException {
        if (existsByUsername(username)) throw new IllegalArgumentException("username_taken");
        if (existsByEmail(email)) throw new IllegalArgumentException("email_taken");

        String hash = BCrypt.hashpw(password, BCrypt.gensalt(12));
        String sql = "INSERT INTO users (username, email, password_hash, full_name, role, is_active) " +
                     "VALUES (?, ?, ?, ?, 'member', TRUE)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, email);
            ps.setString(3, hash);
            ps.setString(4, fullName);
            ps.executeUpdate();
            try (ResultSet gen = ps.getGeneratedKeys()) {
                gen.next();
                int id = gen.getInt(1);
                User u = new User();
                u.setUserId(id);
                u.setUsername(username);
                u.setEmail(email);
                u.setFullName(fullName);
                u.setRole("member");
                u.setActive(true);
                String token = UUID.randomUUID().toString();
                insertSession(c, id, token);
                u.setSessionToken(token);
                return u;
            }
        }
    }

    public User validateToken(String token) throws SQLException {
        String sql = "SELECT u.* FROM users u " +
                     "JOIN sessions s ON s.user_id = u.user_id " +
                     "WHERE s.token = ? AND s.expires_at > NOW() AND u.is_active = TRUE";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, token);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return mapUser(rs);
            }
        }
    }

    public void logout(String token) throws SQLException {
        String sql = "DELETE FROM sessions WHERE token = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, token);
            ps.executeUpdate();
        }
    }

    public List<User> getAllUsers() throws SQLException {
        String sql = "SELECT * FROM users WHERE is_active = TRUE ORDER BY full_name";
        List<User> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapUser(rs));
        }
        return list;
    }

    public List<User> getUsersByProject(int projectId) throws SQLException {
        String sql = "SELECT u.* FROM users u " +
                     "JOIN project_members pm ON pm.user_id = u.user_id " +
                     "WHERE pm.project_id = ?";
        List<User> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapUser(rs));
            }
        }
        return list;
    }

    public boolean updateProfile(int userId, String fullName, String email) throws SQLException {
        String sql = "UPDATE users SET full_name = ?, email = ? WHERE user_id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, fullName);
            ps.setString(2, email);
            ps.setInt(3, userId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean changePassword(int userId, String oldPassword, String newPassword)
            throws SQLException {
        String sql = "SELECT password_hash FROM users WHERE user_id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;
                if (!BCrypt.checkpw(oldPassword, rs.getString("password_hash"))) return false;
            }
        }
        String upd = "UPDATE users SET password_hash = ? WHERE user_id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(upd)) {
            ps.setString(1, BCrypt.hashpw(newPassword, BCrypt.gensalt(12)));
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private boolean existsByUsername(String username) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT 1 FROM users WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    private boolean existsByEmail(String email) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT 1 FROM users WHERE email = ?")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    private void insertSession(Connection c, int userId, String token) throws SQLException {
        String sql = "INSERT INTO sessions (user_id, token, expires_at) " +
                     "VALUES (?, ?, DATE_ADD(NOW(), INTERVAL 30 DAY))";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, token);
            ps.executeUpdate();
        }
    }

    private void updateLastLogin(Connection c, int userId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE users SET last_login = NOW() WHERE user_id = ?")) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getInt("user_id"));
        u.setUsername(rs.getString("username"));
        u.setEmail(rs.getString("email"));
        u.setFullName(rs.getString("full_name"));
        u.setAvatarUrl(rs.getString("avatar_url"));
        u.setRole(rs.getString("role"));
        u.setActive(rs.getBoolean("is_active"));
        Timestamp ll = rs.getTimestamp("last_login");
        if (ll != null) u.setLastLogin(new java.util.Date(ll.getTime()));
        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) u.setCreatedAt(new java.util.Date(ca.getTime()));
        return u;
    }
}
