package mongolup.server.model;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class Project implements Serializable {
    private static final long serialVersionUID = 1L;

    private int projectId;
    private int ownerId;
    private String name;
    private String description;
    private String color;
    private String visibility;
    private boolean isArchived;
    private Date createdAt;

    // computed / joined fields
    private int taskCount;
    private int doneCount;
    private List<User> members;

    public Project() {}

    public int getProjectId()           { return projectId; }
    public void setProjectId(int v)     { projectId = v; }

    public int getOwnerId()             { return ownerId; }
    public void setOwnerId(int v)       { ownerId = v; }

    public String getName()             { return name; }
    public void setName(String v)       { name = v; }

    public String getDescription()      { return description; }
    public void setDescription(String v){ description = v; }

    public String getColor()            { return color; }
    public void setColor(String v)      { color = v; }

    public String getVisibility()       { return visibility; }
    public void setVisibility(String v) { visibility = v; }

    public boolean isArchived()         { return isArchived; }
    public void setArchived(boolean v)  { isArchived = v; }

    public Date getCreatedAt()          { return createdAt; }
    public void setCreatedAt(Date v)    { createdAt = v; }

    public int getTaskCount()           { return taskCount; }
    public void setTaskCount(int v)     { taskCount = v; }

    public int getDoneCount()           { return doneCount; }
    public void setDoneCount(int v)     { doneCount = v; }

    public List<User> getMembers()          { return members; }
    public void setMembers(List<User> v)    { members = v; }

    public double getProgress() {
        return taskCount == 0 ? 0 : (double) doneCount / taskCount * 100;
    }

    @Override
    public String toString() { return name; }
}
