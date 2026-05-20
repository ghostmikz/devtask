package mongolup.server.dao;

import mongolup.server.db.DatabaseConnection;
import mongolup.server.model.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TaskDAO {

    public List<Task> getTasksByProject(int projectId) throws SQLException {
        List<Task> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             CallableStatement cs = c.prepareCall("{CALL sp_get_tasks_by_project(?)}")) {
            cs.setInt(1, projectId);
            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) list.add(mapTask(rs));
            }
        }
        return list;
    }

    public List<Task> getMyTasks(int userId) throws SQLException {
        List<Task> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             CallableStatement cs = c.prepareCall("{CALL sp_get_my_tasks(?)}")) {
            cs.setInt(1, userId);
            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) list.add(mapTask(rs));
            }
        }
        return list;
    }

    public TaskDetail getTaskDetail(int taskId) throws SQLException {
        TaskDetail td = null;
        try (Connection c = DatabaseConnection.getConnection();
             CallableStatement cs = c.prepareCall("{CALL sp_get_task_detail(?)}")) {
            cs.setInt(1, taskId);
            // Close the ResultSet BEFORE opening a second statement on the same connection.
            // MySQL Connector/J can throw if a second statement is issued while an RS is open.
            try (ResultSet rs = cs.executeQuery()) {
                if (!rs.next()) return null;
                td = new TaskDetail();
                copyTaskFields(rs, td);
            } // rs closed here — safe to reuse connection
            td.setAssignees(getAssignees(c, taskId));
        }
        // Other DAOs open their own connections; order doesn't matter
        td.setLabels(new LabelDAO().getLabelsByTask(taskId));
        td.setComments(new CommentDAO().getCommentsByTask(taskId));
        td.setAttachments(new AttachmentDAO().getAttachmentsByTask(taskId));
        return td;
    }

    public Task createTask(Task task) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             CallableStatement cs = c.prepareCall("{CALL sp_create_task(?,?,?,?,?,?,?,?,?,?)}")) {
            cs.setInt(1, task.getProjectId());
            cs.setInt(2, task.getCreatedBy());
            if (task.getStatusId() != null) cs.setInt(3, task.getStatusId());
            else                            cs.setNull(3, Types.INTEGER);
            cs.setString(4, task.getTitle());
            cs.setString(5, task.getDescription());
            cs.setInt(6, Math.max(1, task.getWeight()));
            cs.setInt(7, Math.max(1, task.getPriority()));
            if (task.getDueDate() != null) cs.setTimestamp(8, new Timestamp(task.getDueDate().getTime()));
            else                           cs.setNull(8, Types.TIMESTAMP);
            if (task.getStartDate() != null) cs.setTimestamp(9, new Timestamp(task.getStartDate().getTime()));
            else                             cs.setNull(9, Types.TIMESTAMP);
            cs.registerOutParameter(10, Types.INTEGER);
            cs.execute();
            task.setTaskId(cs.getInt(10));
        }
        return task;
    }

    public boolean updateTask(Task task) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             CallableStatement cs = c.prepareCall("{CALL sp_update_task(?,?,?,?,?,?,?,?)}")) {
            cs.setInt(1, task.getTaskId());
            cs.setString(2, task.getTitle());
            cs.setString(3, task.getDescription());
            cs.setInt(4, task.getWeight());
            cs.setInt(5, task.getPriority());
            if (task.getDueDate() != null) cs.setTimestamp(6, new Timestamp(task.getDueDate().getTime()));
            else                           cs.setNull(6, Types.TIMESTAMP);
            if (task.getStatusId() != null) cs.setInt(7, task.getStatusId());
            else                            cs.setNull(7, Types.INTEGER);
            cs.registerOutParameter(8, Types.INTEGER);
            cs.execute();
            return cs.getInt(8) > 0;
        }
    }

    public boolean updateTaskStatus(int taskId, int statusId, int userId) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             CallableStatement cs = c.prepareCall("{CALL sp_update_task_status(?,?,?,?)}")) {
            cs.setInt(1, taskId);
            cs.setInt(2, statusId);
            cs.setInt(3, userId);
            cs.registerOutParameter(4, Types.INTEGER);
            cs.execute();
            return cs.getInt(4) > 0;
        }
    }

    public boolean markTaskDone(int taskId, int projectId) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             CallableStatement cs = c.prepareCall("{CALL sp_mark_task_done(?,?,?)}")) {
            cs.setInt(1, taskId);
            cs.setInt(2, projectId);
            cs.registerOutParameter(3, Types.INTEGER);
            cs.execute();
            return cs.getInt(3) > 0;
        }
    }

    public boolean deleteTask(int taskId) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             CallableStatement cs = c.prepareCall("{CALL sp_delete_task(?,?)}")) {
            cs.setInt(1, taskId);
            cs.registerOutParameter(2, Types.INTEGER);
            cs.execute();
            return cs.getInt(2) > 0;
        }
    }

    public boolean assignUser(int taskId, int userId) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             CallableStatement cs = c.prepareCall("{CALL sp_assign_user(?,?,?)}")) {
            cs.setInt(1, taskId);
            cs.setInt(2, userId);
            cs.registerOutParameter(3, Types.INTEGER);
            cs.execute();
            return cs.getInt(3) > 0;
        }
    }

    public boolean removeAssignee(int taskId, int userId) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             CallableStatement cs = c.prepareCall("{CALL sp_remove_assignee(?,?)}")) {
            cs.setInt(1, taskId);
            cs.setInt(2, userId);
            cs.execute();
            return true;
        }
    }

    public List<Object[]> getReportData(int projectId) throws SQLException {
        List<Object[]> rows = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             CallableStatement cs = c.prepareCall("{CALL sp_get_report_data(?)}")) {
            cs.setInt(1, projectId);
            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    rows.add(new Object[]{
                        rs.getString("name"),
                        rs.getInt("cnt"),
                        rs.getString("color")
                    });
                }
            }
        }
        return rows;
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private List<User> getAssignees(Connection c, int taskId) throws SQLException {
        List<User> list = new ArrayList<>();
        try (CallableStatement cs = c.prepareCall("{CALL sp_get_assignees(?)}")) {
            cs.setInt(1, taskId);
            try (ResultSet rs = cs.executeQuery()) {
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
        try { t.setStatusName(rs.getString("status_name"));   } catch (SQLException ignored) {}
        try { t.setStatusColor(rs.getString("status_color")); } catch (SQLException ignored) {}
        try { t.setStatusType(rs.getString("status_type"));   } catch (SQLException ignored) {}
    }
}
