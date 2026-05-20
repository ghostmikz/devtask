package mongolup.server.model;

import java.io.Serializable;
import java.util.List;

public class TaskDetail extends Task implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<User>       assignees;
    private List<Label>      labels;
    private List<Comment>    comments;
    private List<Attachment> attachments;

    public TaskDetail() {}

    public List<User> getAssignees()                  { return assignees; }
    public void setAssignees(List<User> v)            { assignees = v; }

    public List<Label> getLabels()                    { return labels; }
    public void setLabels(List<Label> v)              { labels = v; }

    public List<Comment> getComments()                { return comments; }
    public void setComments(List<Comment> v)          { comments = v; }

    public List<Attachment> getAttachments()          { return attachments; }
    public void setAttachments(List<Attachment> v)    { attachments = v; }
}
