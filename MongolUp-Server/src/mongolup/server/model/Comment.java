package mongolup.server.model;

import java.io.Serializable;
import java.util.Date;

public class Comment implements Serializable {
    private static final long serialVersionUID = 1L;

    private int commentId;
    private int taskId;
    private int userId;
    private Integer parentCommentId;
    private String content;
    private boolean isEdited;
    private Date createdAt;

    // joined
    private String authorName;
    private String authorInitials;

    public Comment() {}

    public int getCommentId()               { return commentId; }
    public void setCommentId(int v)         { commentId = v; }

    public int getTaskId()                  { return taskId; }
    public void setTaskId(int v)            { taskId = v; }

    public int getUserId()                  { return userId; }
    public void setUserId(int v)            { userId = v; }

    public Integer getParentCommentId()         { return parentCommentId; }
    public void setParentCommentId(Integer v)   { parentCommentId = v; }

    public String getContent()              { return content; }
    public void setContent(String v)        { content = v; }

    public boolean isEdited()               { return isEdited; }
    public void setEdited(boolean v)        { isEdited = v; }

    public Date getCreatedAt()              { return createdAt; }
    public void setCreatedAt(Date v)        { createdAt = v; }

    public String getAuthorName()           { return authorName; }
    public void setAuthorName(String v)     { authorName = v; }

    public String getAuthorInitials()       { return authorInitials; }
    public void setAuthorInitials(String v) { authorInitials = v; }
}
