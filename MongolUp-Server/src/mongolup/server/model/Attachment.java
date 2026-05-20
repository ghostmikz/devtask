package mongolup.server.model;

import java.io.Serializable;
import java.util.Date;

public class Attachment implements Serializable {
    private static final long serialVersionUID = 1L;

    private int    attachmentId;
    private int    taskId;
    private int    uploadedBy;
    private String filename;
    private String mimeType;
    private int    fileSize;
    private byte[] fileData;      // null in list responses; populated only on download
    private Date   createdAt;
    private String uploaderName;

    public Attachment() {}

    public int    getAttachmentId()         { return attachmentId; }
    public void   setAttachmentId(int v)    { attachmentId = v; }

    public int    getTaskId()               { return taskId; }
    public void   setTaskId(int v)          { taskId = v; }

    public int    getUploadedBy()           { return uploadedBy; }
    public void   setUploadedBy(int v)      { uploadedBy = v; }

    public String getFilename()             { return filename; }
    public void   setFilename(String v)     { filename = v; }

    public String getMimeType()             { return mimeType; }
    public void   setMimeType(String v)     { mimeType = v; }

    public int    getFileSize()             { return fileSize; }
    public void   setFileSize(int v)        { fileSize = v; }

    public byte[] getFileData()             { return fileData; }
    public void   setFileData(byte[] v)     { fileData = v; }

    public Date   getCreatedAt()            { return createdAt; }
    public void   setCreatedAt(Date v)      { createdAt = v; }

    public String getUploaderName()         { return uploaderName; }
    public void   setUploaderName(String v) { uploaderName = v; }

    /** Human-readable file size (e.g. "1.2 MB", "340 KB", "800 B") */
    public String getFileSizeDisplay() {
        if (fileSize < 1024)              return fileSize + " B";
        if (fileSize < 1024 * 1024)       return (fileSize / 1024) + " KB";
        return String.format("%.1f MB", fileSize / (1024.0 * 1024));
    }

    @Override
    public String toString() { return filename != null ? filename : "attachment"; }
}
