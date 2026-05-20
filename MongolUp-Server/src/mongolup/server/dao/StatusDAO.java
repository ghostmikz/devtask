package mongolup.server.dao;

import mongolup.server.db.DatabaseConnection;
import mongolup.server.model.Status;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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
