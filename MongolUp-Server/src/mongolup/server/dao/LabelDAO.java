package mongolup.server.dao;

import mongolup.server.db.DatabaseConnection;
import mongolup.server.model.Label;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LabelDAO {

    public List<Label> getLabelsByProject(int projectId) throws SQLException {
        List<Label> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             CallableStatement cs = c.prepareCall("{CALL sp_get_labels_by_project(?)}")) {
            cs.setInt(1, projectId);
            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) list.add(mapLabel(rs));
            }
        }
        return list;
    }

    public List<Label> getLabelsByTask(int taskId) throws SQLException {
        List<Label> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             CallableStatement cs = c.prepareCall("{CALL sp_get_labels_by_task(?)}")) {
            cs.setInt(1, taskId);
            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) list.add(mapLabel(rs));
            }
        }
        return list;
    }

    public boolean addLabelToTask(int taskId, int labelId) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             CallableStatement cs = c.prepareCall("{CALL sp_add_label_to_task(?,?)}")) {
            cs.setInt(1, taskId);
            cs.setInt(2, labelId);
            cs.execute();
            return true;
        }
    }

    public boolean removeLabelFromTask(int taskId, int labelId) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             CallableStatement cs = c.prepareCall("{CALL sp_remove_label_from_task(?,?)}")) {
            cs.setInt(1, taskId);
            cs.setInt(2, labelId);
            cs.execute();
            return true;
        }
    }

    public Label createLabel(int projectId, String name, String color) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             CallableStatement cs = c.prepareCall("{CALL sp_create_label(?,?,?,?)}")) {
            cs.setInt(1, projectId);
            cs.setString(2, name);
            cs.setString(3, color);
            cs.registerOutParameter(4, Types.INTEGER);
            cs.execute();
            Label l = new Label();
            l.setLabelId(cs.getInt(4));
            l.setProjectId(projectId);
            l.setName(name);
            l.setColor(color);
            return l;
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
