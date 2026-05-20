package mongolup.server.dao;

import mongolup.server.db.DatabaseConnection;
import mongolup.server.model.Comment;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CommentDAO {

    public List<Comment> getCommentsByTask(int taskId) throws SQLException {
        String sql =
            "SELECT c.*, u.full_name AS author_name, u.username " +
            "FROM comments c JOIN users u ON u.user_id = c.user_id " +
            "WHERE c.task_id = ? ORDER BY c.created_at ASC";
        List<Comment> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, taskId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapComment(rs));
            }
        }
        return list;
    }

    public Comment addComment(int taskId, int userId, String content) throws SQLException {
        String sql = "INSERT INTO comments (task_id, user_id, content) VALUES (?, ?, ?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, taskId);
            ps.setInt(2, userId);
            ps.setString(3, content);
            ps.executeUpdate();
            try (ResultSet gen = ps.getGeneratedKeys()) {
                gen.next();
                int commentId = gen.getInt(1);
                // fetch back with author info
                return getComment(c, commentId);
            }
        }
    }

    private Comment getComment(Connection c, int commentId) throws SQLException {
        String sql =
            "SELECT cm.*, u.full_name AS author_name, u.username " +
            "FROM comments cm JOIN users u ON u.user_id = cm.user_id " +
            "WHERE cm.comment_id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, commentId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return mapComment(rs);
            }
        }
    }

    private Comment mapComment(ResultSet rs) throws SQLException {
        Comment cm = new Comment();
        cm.setCommentId(rs.getInt("comment_id"));
        cm.setTaskId(rs.getInt("task_id"));
        cm.setUserId(rs.getInt("user_id"));
        cm.setContent(rs.getString("content"));
        cm.setEdited(rs.getBoolean("is_edited"));
        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) cm.setCreatedAt(new java.util.Date(ca.getTime()));
        String authorName = rs.getString("author_name");
        if (authorName == null || authorName.isBlank())
            authorName = rs.getString("username");
        cm.setAuthorName(authorName);
        // initials
        String[] parts = authorName.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) if (!p.isEmpty()) sb.appendCodePoint(p.codePointAt(0));
        cm.setAuthorInitials(sb.toString().toUpperCase());
        return cm;
    }
}
