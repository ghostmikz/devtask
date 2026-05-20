package mongolup.server.dao;

import mongolup.server.db.DatabaseConnection;
import mongolup.server.model.Project;
import mongolup.server.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProjectDAO {

    public List<Project> getProjectsForUser(int userId) throws SQLException {
        String sql =
            "SELECT p.*, " +
            "  (SELECT COUNT(*) FROM tasks t WHERE t.project_id = p.project_id) AS task_count, " +
            "  (SELECT COUNT(*) FROM tasks t " +
            "   JOIN statuses s ON s.status_id = t.status_id " +
            "   WHERE t.project_id = p.project_id AND s.type = 'done') AS done_count " +
            "FROM projects p " +
            "JOIN project_members pm ON pm.project_id = p.project_id " +
            "WHERE pm.user_id = ? AND p.is_archived = FALSE " +
            "ORDER BY p.created_at DESC";
        List<Project> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Project p = mapProject(rs);
                    p.setMembers(getMembersOf(c, p.getProjectId()));
                    list.add(p);
                }
            }
        }
        return list;
    }

    public Project createProject(int ownerId, String name, String description, String color)
            throws SQLException {
        String sql = "INSERT INTO projects (owner_id, name, description, color, visibility) " +
                     "VALUES (?, ?, ?, ?, 'private')";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, ownerId);
            ps.setString(2, name);
            ps.setString(3, description);
            ps.setString(4, color != null ? color : "#378ADD");
            ps.executeUpdate();
            try (ResultSet gen = ps.getGeneratedKeys()) {
                gen.next();
                int projectId = gen.getInt(1);
                // add owner as manager member
                addMember(c, projectId, ownerId, "manager");
                // create default statuses
                createDefaultStatuses(c, projectId);
                Project p = new Project();
                p.setProjectId(projectId);
                p.setOwnerId(ownerId);
                p.setName(name);
                p.setDescription(description);
                p.setColor(color != null ? color : "#378ADD");
                p.setVisibility("private");
                return p;
            }
        }
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private List<User> getMembersOf(Connection c, int projectId) throws SQLException {
        String sql = "SELECT u.user_id, u.username, u.full_name, u.avatar_url " +
                     "FROM users u JOIN project_members pm ON pm.user_id = u.user_id " +
                     "WHERE pm.project_id = ?";
        List<User> members = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    User u = new User();
                    u.setUserId(rs.getInt("user_id"));
                    u.setUsername(rs.getString("username"));
                    u.setFullName(rs.getString("full_name"));
                    u.setAvatarUrl(rs.getString("avatar_url"));
                    members.add(u);
                }
            }
        }
        return members;
    }

    /** Public: add a member to a project (called by ClientHandler). */
    public boolean addProjectMember(int projectId, int userId) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection()) {
            addMember(c, projectId, userId, "member");
            return true;
        }
    }

    /** Public: remove a member from a project (cannot remove owner). */
    public boolean removeProjectMember(int projectId, int userId) throws SQLException {
        String sql = "DELETE FROM project_members WHERE project_id=? AND user_id=? AND role != 'manager'";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    private void addMember(Connection c, int projectId, int userId, String role)
            throws SQLException {
        String sql = "INSERT IGNORE INTO project_members (project_id, user_id, role) VALUES (?,?,?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            ps.setInt(2, userId);
            ps.setString(3, role);
            ps.executeUpdate();
        }
    }

    private void createDefaultStatuses(Connection c, int projectId) throws SQLException {
        String sql = "INSERT INTO statuses (project_id, name, color, type, sort_order) VALUES (?,?,?,?,?)";
        Object[][] defaults = {
            {"Шинэ",           "#9CA3AF", "todo",        0},
            {"Хийгдэж байгаа", "#3B82F6", "in_progress", 1},
            {"Testing",        "#F59E0B", "review",      2},
            {"Дууссан",        "#0F6E56", "done",        3}
        };
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (Object[] row : defaults) {
                ps.setInt(1, projectId);
                ps.setString(2, (String) row[0]);
                ps.setString(3, (String) row[1]);
                ps.setString(4, (String) row[2]);
                ps.setInt(5, (int) row[3]);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private Project mapProject(ResultSet rs) throws SQLException {
        Project p = new Project();
        p.setProjectId(rs.getInt("project_id"));
        p.setOwnerId(rs.getInt("owner_id"));
        p.setName(rs.getString("name"));
        p.setDescription(rs.getString("description"));
        p.setColor(rs.getString("color"));
        p.setVisibility(rs.getString("visibility"));
        p.setArchived(rs.getBoolean("is_archived"));
        p.setTaskCount(rs.getInt("task_count"));
        p.setDoneCount(rs.getInt("done_count"));
        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) p.setCreatedAt(new java.util.Date(ca.getTime()));
        return p;
    }
}
