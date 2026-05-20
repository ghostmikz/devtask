package mongolup.server.dao;

import mongolup.server.db.DatabaseConnection;
import mongolup.server.model.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TaskDAO {

    public List<Task> getTasksByProject(int projectId) throws SQLException {
        String sql =
            "SELECT t.*, s.name AS status_name, s.color AS status_color, s.type AS status_type " +
            "FROM tasks t LEFT JOIN statuses s ON s.status_id = t.status_id " +
            "WHERE t.project_id = ? ORDER BY t.created_at DESC";
        List<Task> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapTask(rs));
            }
        }
        return list;
    }

    public List<Task> getMyTasks(int userId) throws SQLException {
        String sql =
            "SELECT t.*, s.name AS status_name, s.color AS status_color, s.type AS status_type " +
            "FROM tasks t " +
            "LEFT JOIN statuses s ON s.status_id = t.status_id " +
            "JOIN task_assignments ta ON ta.task_id = t.task_id " +
            "WHERE ta.user_id = ? ORDER BY t.priority DESC, t.due_date ASC";
        List<Task> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapTask(rs));
            }
        }
        return list;
    }

    public TaskDetail getTaskDetail(int taskId) throws SQLException {
        String sql =
            "SELECT t.*, s.name AS status_name, s.color AS status_color, s.type AS status_type " +
            "FROM tasks t LEFT JOIN statuses s ON s.status_id = t.status_id " +
            "WHERE t.task_id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, taskId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                TaskDetail td = new TaskDetail();
                copyTaskFields(rs, td);
                td.setAssignees(getAssignees(c, taskId));
                td.setLabels(new LabelDAO().getLabelsByTask(taskId));
                td.setComments(new CommentDAO().getCommentsByTask(taskId));
                return td;
            }
        }
    }

    public Task createTask(Task task) throws SQLException {
        String sql =
            "INSERT INTO tasks (project_id, created_by, status_id, title, description, " +
            "weight, priority, due_date, start_date) VALUES (?,?,?,?,?,?,?,?,?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, task.getProjectId());
            ps.setInt(2, task.getCreatedBy());
            if (task.getStatusId() != null) ps.setInt(3, task.getStatusId());
            else ps.setNull(3, Types.INTEGER);
            ps.setString(4, task.getTitle());
            ps.setString(5, task.getDescription());
            ps.setInt(6, Math.max(1, task.getWeight()));
            ps.setInt(7, Math.max(1, task.getPriority()));
            if (task.getDueDate() != null) ps.setTimestamp(8, new Timestamp(task.getDueDate().getTime()));
            else ps.setNull(8, Types.TIMESTAMP);
            if (task.getStartDate() != null) ps.setTimestamp(9, new Timestamp(task.getStartDate().getTime()));
            else ps.setNull(9, Types.TIMESTAMP);
            ps.executeUpdate();
            try (ResultSet gen = ps.getGeneratedKeys()) {
                gen.next();
                task.setTaskId(gen.getInt(1));
            }
        }
        // log activity
        logActivity(task.getTaskId(), task.getCreatedBy(), "task_created", null, task.getTitle());
        return task;
    }

    public boolean updateTask(Task task) throws SQLException {
        String sql =
            "UPDATE tasks SET title=?, description=?, weight=?, priority=?, due_date=?, " +
            "status_id=?, updated_at=NOW() WHERE task_id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, task.getTitle());
            ps.setString(2, task.getDescription());
            ps.setInt(3, task.getWeight());
            ps.setInt(4, task.getPriority());
            if (task.getDueDate() != null) ps.setTimestamp(5, new Timestamp(task.getDueDate().getTime()));
            else ps.setNull(5, Types.TIMESTAMP);
            if (task.getStatusId() != null) ps.setInt(6, task.getStatusId());
            else ps.setNull(6, Types.INTEGER);
            ps.setInt(7, task.getTaskId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean updateTaskStatus(int taskId, int statusId, int userId) throws SQLException {
        // get old status for log
        String oldType = getStatusType(taskId);
        String sql = "UPDATE tasks SET status_id = ?, updated_at = NOW() WHERE task_id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, statusId);
            ps.setInt(2, taskId);
            boolean ok = ps.executeUpdate() > 0;
            if (ok) {
                String newName = getStatusNameById(c, statusId);
                logActivity(taskId, userId, "status_changed", oldType, newName);
            }
            return ok;
        }
    }

    public boolean assignUser(int taskId, int userId) throws SQLException {
        String sql = "INSERT IGNORE INTO task_assignments (task_id, user_id) VALUES (?, ?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, taskId);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean markTaskDone(int taskId, int projectId) throws SQLException {
        // Find the 'done'-type status for this project
        String sql =
            "UPDATE tasks SET status_id = (" +
            "  SELECT status_id FROM statuses " +
            "  WHERE project_id = ? AND type = 'done' " +
            "  ORDER BY sort_order DESC LIMIT 1" +
            "), updated_at = NOW() " +
            "WHERE task_id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            ps.setInt(2, taskId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteTask(int taskId) throws SQLException {
        // Remove child rows first to avoid FK violations
        try (Connection c = DatabaseConnection.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(
                    "DELETE FROM task_assignments WHERE task_id = ?")) {
                ps.setInt(1, taskId); ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "DELETE FROM task_labels WHERE task_id = ?")) {
                ps.setInt(1, taskId); ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "DELETE FROM activity_logs WHERE task_id = ?")) {
                ps.setInt(1, taskId); ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "DELETE FROM comments WHERE task_id = ?")) {
                ps.setInt(1, taskId); ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "DELETE FROM tasks WHERE task_id = ?")) {
                ps.setInt(1, taskId);
                return ps.executeUpdate() > 0;
            }
        }
    }

    public boolean removeAssignee(int taskId, int userId) throws SQLException {
        String sql = "DELETE FROM task_assignments WHERE task_id = ? AND user_id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, taskId);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    public List<Object[]> getReportData(int projectId) throws SQLException {
        // Row format: [statusName, count, color]
        // First row is a special "__overdue__" row with the real overdue count.
        List<Object[]> rows = new ArrayList<>();

        // Overdue count: due_date in the past, status not 'done'
        String overdueSql =
            "SELECT COUNT(*) FROM tasks t " +
            "LEFT JOIN statuses s ON s.status_id = t.status_id " +
            "WHERE t.project_id = ? AND t.due_date < NOW() " +
            "AND (s.type IS NULL OR s.type != 'done')";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(overdueSql)) {
            ps.setInt(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                int overdue = rs.next() ? rs.getInt(1) : 0;
                rows.add(new Object[]{"__overdue__", overdue, null});
            }
        }

        // Status counts
        String sql =
            "SELECT s.name, COUNT(t.task_id) AS cnt, s.color " +
            "FROM statuses s LEFT JOIN tasks t ON t.status_id = s.status_id " +
            "WHERE s.project_id = ? GROUP BY s.status_id ORDER BY s.sort_order";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new Object[]{rs.getString("name"), rs.getInt("cnt"), rs.getString("color")});
                }
            }
        }
        return rows;
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private List<User> getAssignees(Connection c, int taskId) throws SQLException {
        String sql =
            "SELECT u.user_id, u.username, u.full_name, u.avatar_url " +
            "FROM users u JOIN task_assignments ta ON ta.user_id = u.user_id " +
            "WHERE ta.task_id = ?";
        List<User> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, taskId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    User u = new User();
                    u.setUserId(rs.getInt("user_id"));
                    u.setUsername(rs.getString("username"));
                    u.setFullName(rs.getString("full_name"));
                    u.setAvatarUrl(rs.getString("avatar_url"));
                    list.add(u);
                }
            }
        }
        return list;
    }

    private String getStatusType(int taskId) throws SQLException {
        String sql = "SELECT s.type FROM statuses s JOIN tasks t ON t.status_id = s.status_id WHERE t.task_id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, taskId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("type") : null;
            }
        }
    }

    private String getStatusNameById(Connection c, int statusId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT name FROM statuses WHERE status_id = ?")) {
            ps.setInt(1, statusId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("name") : "";
            }
        }
    }

    private void logActivity(int taskId, int userId, String action, String oldVal, String newVal)
            throws SQLException {
        String sql = "INSERT INTO activity_logs (task_id, user_id, action, old_value, new_value) VALUES (?,?,?,?,?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, taskId);
            ps.setInt(2, userId);
            ps.setString(3, action);
            ps.setString(4, oldVal);
            ps.setString(5, newVal);
            ps.executeUpdate();
        }
    }

    private Task mapTask(ResultSet rs) throws SQLException {
        Task t = new Task();
        copyTaskFields(rs, t);
        return t;
    }

    private void copyTaskFields(ResultSet rs, Task t) throws SQLException {
        t.setTaskId(rs.getInt("task_id"));
        t.setProjectId(rs.getInt("project_id"));
        int ptid = rs.getInt("parent_task_id");
        if (!rs.wasNull()) t.setParentTaskId(ptid);
        t.setCreatedBy(rs.getInt("created_by"));
        int sid = rs.getInt("status_id");
        if (!rs.wasNull()) t.setStatusId(sid);
        t.setTitle(rs.getString("title"));
        t.setDescription(rs.getString("description"));
        t.setWeight(rs.getInt("weight"));
        t.setPriority(rs.getInt("priority"));
        Timestamp dd = rs.getTimestamp("due_date");
        if (dd != null) t.setDueDate(new java.util.Date(dd.getTime()));
        Timestamp sd = rs.getTimestamp("start_date");
        if (sd != null) t.setStartDate(new java.util.Date(sd.getTime()));
        Timestamp ca = rs.getTimestamp("completed_at");
        if (ca != null) t.setCompletedAt(new java.util.Date(ca.getTime()));
        Timestamp cr = rs.getTimestamp("created_at");
        if (cr != null) t.setCreatedAt(new java.util.Date(cr.getTime()));
        Timestamp upd = rs.getTimestamp("updated_at");
        if (upd != null) t.setUpdatedAt(new java.util.Date(upd.getTime()));
        // joined status fields (may be null if LEFT JOIN found nothing)
        try { t.setStatusName(rs.getString("status_name")); } catch (SQLException ignored) {}
        try { t.setStatusColor(rs.getString("status_color")); } catch (SQLException ignored) {}
        try { t.setStatusType(rs.getString("status_type")); } catch (SQLException ignored) {}
    }
}
