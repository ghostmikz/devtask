package mongolup.server.dao;

import mongolup.server.db.DatabaseConnection;
import mongolup.server.model.Status;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
// Statement already included via java.sql.*

public class StatusDAO {

    public List<Status> getStatusesByProject(int projectId) throws SQLException {
        String sql = "SELECT * FROM statuses WHERE project_id = ? ORDER BY sort_order";
        List<Status> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapStatus(rs));
            }
        }
        return list;
    }

    public Status createStatus(int projectId, String name, String color) throws SQLException {
        String sql = "INSERT INTO statuses (project_id, name, color, type, sort_order) " +
                     "VALUES (?, ?, ?, 'todo', " +
                     "  (SELECT COALESCE(MAX(s2.sort_order),0)+1 FROM statuses s2 WHERE s2.project_id=?))";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, projectId);
            ps.setString(2, name);
            ps.setString(3, color != null ? color : "#9CA3AF");
            ps.setInt(4, projectId);
            ps.executeUpdate();
            try (ResultSet gen = ps.getGeneratedKeys()) {
                gen.next();
                Status s = new Status();
                s.setStatusId(gen.getInt(1));
                s.setProjectId(projectId);
                s.setName(name);
                s.setColor(color != null ? color : "#9CA3AF");
                s.setType("todo");
                return s;
            }
        }
    }

    public boolean renameStatus(int statusId, String newName) throws SQLException {
        String sql = "UPDATE statuses SET name = ? WHERE status_id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, newName);
            ps.setInt(2, statusId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteStatus(int statusId) throws SQLException {
        // Unassign tasks from this status before deleting
        try (Connection c = DatabaseConnection.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE tasks SET status_id = NULL WHERE status_id = ?")) {
                ps.setInt(1, statusId); ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "DELETE FROM statuses WHERE status_id = ?")) {
                ps.setInt(1, statusId);
                return ps.executeUpdate() > 0;
            }
        }
    }

    private Status mapStatus(ResultSet rs) throws SQLException {
        Status s = new Status();
        s.setStatusId(rs.getInt("status_id"));
        s.setProjectId(rs.getInt("project_id"));
        s.setName(rs.getString("name"));
        s.setColor(rs.getString("color"));
        s.setType(rs.getString("type"));
        s.setSortOrder(rs.getInt("sort_order"));
        return s;
    }
}
