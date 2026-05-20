package mongolup.server.dao;

import mongolup.server.db.DatabaseConnection;
import mongolup.server.model.Label;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LabelDAO {

    public List<Label> getLabelsByProject(int projectId) throws SQLException {
        String sql = "SELECT * FROM labels WHERE project_id = ? ORDER BY name";
        List<Label> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapLabel(rs));
            }
        }
        return list;
    }

    public List<Label> getLabelsByTask(int taskId) throws SQLException {
        String sql = "SELECT l.* FROM labels l " +
                     "JOIN task_labels tl ON tl.label_id = l.label_id " +
                     "WHERE tl.task_id = ?";
        List<Label> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, taskId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapLabel(rs));
            }
        }
        return list;
    }

    public boolean addLabelToTask(int taskId, int labelId) throws SQLException {
        String sql = "INSERT IGNORE INTO task_labels (task_id, label_id) VALUES (?, ?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, taskId);
            ps.setInt(2, labelId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean removeLabelFromTask(int taskId, int labelId) throws SQLException {
        String sql = "DELETE FROM task_labels WHERE task_id = ? AND label_id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, taskId);
            ps.setInt(2, labelId);
            return ps.executeUpdate() > 0;
        }
    }

    public Label createLabel(int projectId, String name, String color) throws SQLException {
        String sql = "INSERT INTO labels (project_id, name, color) VALUES (?, ?, ?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, projectId);
            ps.setString(2, name);
            ps.setString(3, color);
            ps.executeUpdate();
            try (ResultSet gen = ps.getGeneratedKeys()) {
                gen.next();
                Label l = new Label();
                l.setLabelId(gen.getInt(1));
                l.setProjectId(projectId);
                l.setName(name);
                l.setColor(color);
                return l;
            }
        }
    }

    private Label mapLabel(ResultSet rs) throws SQLException {
        Label l = new Label();
        l.setLabelId(rs.getInt("label_id"));
        l.setProjectId(rs.getInt("project_id"));
        l.setName(rs.getString("name"));
        l.setColor(rs.getString("color"));
        return l;
    }
}
