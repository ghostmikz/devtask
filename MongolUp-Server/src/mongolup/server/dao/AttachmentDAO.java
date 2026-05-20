package mongolup.server.dao;

import mongolup.server.db.DatabaseConnection;
import mongolup.server.model.Attachment;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AttachmentDAO {

    /** Upload a file and return the saved metadata (no fileData in response). */
    public Attachment addAttachment(int taskId, int userId, String filename,
                                    String mimeType, int fileSize, byte[] data) throws SQLException {
        int id;
        try (Connection c = DatabaseConnection.getConnection();
             CallableStatement cs = c.prepareCall("{CALL sp_add_attachment(?,?,?,?,?,?,?)}")) {
            cs.setInt(1, taskId);
            cs.setInt(2, userId);
            cs.setString(3, filename);
            cs.setString(4, mimeType != null ? mimeType : "application/octet-stream");
            cs.setInt(5, fileSize);
            cs.setBytes(6, data);
            cs.registerOutParameter(7, Types.INTEGER);
            cs.execute();
            id = cs.getInt(7);
        }
        Attachment a = new Attachment();
        a.setAttachmentId(id);
        a.setTaskId(taskId);
        a.setUploadedBy(userId);
        a.setFilename(filename);
        a.setMimeType(mimeType);
        a.setFileSize(fileSize);
        a.setCreatedAt(new java.util.Date());
        return a;
    }

    /** Returns metadata list (no fileData) for all attachments of a task. */
    public List<Attachment> getAttachmentsByTask(int taskId) throws SQLException {
        List<Attachment> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             CallableStatement cs = c.prepareCall("{CALL sp_get_attachments_by_task(?)}")) {
            cs.setInt(1, taskId);
            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) list.add(mapMeta(rs));
            }
        }
        return list;
    }

    /** Returns a single attachment WITH its fileData for download. */
    public Attachment getAttachmentFile(int attachmentId) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             CallableStatement cs = c.prepareCall("{CALL sp_get_attachment_file(?)}")) {
            cs.setInt(1, attachmentId);
            try (ResultSet rs = cs.executeQuery()) {
                if (!rs.next()) return null;
                Attachment a = new Attachment();
                a.setAttachmentId(rs.getInt("attachment_id"));
                a.setFilename(rs.getString("filename"));
                a.setMimeType(rs.getString("mime_type"));
                a.setFileData(rs.getBytes("file_data"));
                return a;
            }
        }
    }

    /** Deletes an attachment only if the requesting user is the uploader. */
    public boolean deleteAttachment(int attachmentId, int userId) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             CallableStatement cs = c.prepareCall("{CALL sp_delete_attachment(?,?,?)}")) {
            cs.setInt(1, attachmentId);
            cs.setInt(2, userId);
            cs.registerOutParameter(3, Types.INTEGER);
            cs.execute();
            return cs.getInt(3) > 0;
        }
    }

    // ── mapping ──────────────────────────────────────────────────────────────

    private Attachment mapMeta(ResultSet rs) throws SQLException {
        Attachment a = new Attachment();
        a.setAttachmentId(rs.getInt("attachment_id"));
        a.setTaskId(rs.getInt("task_id"));
        a.setUploadedBy(rs.getInt("uploaded_by"));
        a.setFilename(rs.getString("filename"));
        a.setMimeType(rs.getString("mime_type"));
        a.setFileSize(rs.getInt("file_size"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) a.setCreatedAt(new java.util.Date(ts.getTime()));
        String name = rs.getString("uploader_name");
        if (name == null || name.isBlank()) name = rs.getString("username");
        a.setUploaderName(name);
        return a;
    }
}
