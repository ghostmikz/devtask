package mongolup.server.model;

import java.io.Serializable;
import java.util.Date;

public class Task implements Serializable {
    private static final long serialVersionUID = 1L;

    private int taskId;
    private int projectId;
    private Integer parentTaskId;
    private int createdBy;
    private Integer statusId;
    private String title;
    private String description;
    private int weight;          // story points 1-10
    private int priority;        // 1=low 2=medium 3=high 4=urgent
    private Date dueDate;
    private Date startDate;
    private Date completedAt;
    private Date createdAt;
    private Date updatedAt;

    // joined / display fields
    private String statusName;
    private String statusColor;
    private String statusType;
    // "userId:Full Name|userId:Full Name" — populated by sp_get_tasks_by_project
    private String assigneesCsv;

    public Task() {}

    public int getTaskId()              { return taskId; }
    public void setTaskId(int v)        { taskId = v; }

    public int getProjectId()           { return projectId; }
    public void setProjectId(int v)     { projectId = v; }

    public Integer getParentTaskId()        { return parentTaskId; }
    public void setParentTaskId(Integer v)  { parentTaskId = v; }

    public int getCreatedBy()           { return createdBy; }
    public void setCreatedBy(int v)     { createdBy = v; }

    public Integer getStatusId()        { return statusId; }
    public void setStatusId(Integer v)  { statusId = v; }

    public String getTitle()            { return title; }
    public void setTitle(String v)      { title = v; }

    public String getDescription()      { return description; }
    public void setDescription(String v){ description = v; }

    public int getWeight()              { return weight; }
    public void setWeight(int v)        { weight = v; }

    public int getPriority()            { return priority; }
    public void setPriority(int v)      { priority = v; }

    public Date getDueDate()            { return dueDate; }
    public void setDueDate(Date v)      { dueDate = v; }

    public Date getStartDate()          { return startDate; }
    public void setStartDate(Date v)    { startDate = v; }

    public Date getCompletedAt()        { return completedAt; }
    public void setCompletedAt(Date v)  { completedAt = v; }

    public Date getCreatedAt()          { return createdAt; }
    public void setCreatedAt(Date v)    { createdAt = v; }

    public Date getUpdatedAt()          { return updatedAt; }
    public void setUpdatedAt(Date v)    { updatedAt = v; }

    public String getStatusName()           { return statusName; }
    public void setStatusName(String v)     { statusName = v; }

    public String getStatusColor()          { return statusColor; }
    public void setStatusColor(String v)    { statusColor = v; }

    public String getStatusType()           { return statusType; }
    public void setStatusType(String v)     { statusType = v; }

    public String getAssigneesCsv()         { return assigneesCsv; }
    public void setAssigneesCsv(String v)   { assigneesCsv = v; }

    public boolean isDone() {
        return "done".equalsIgnoreCase(statusType);
    }

    /** Priority color matching HTML design */
    public String getPriorityColor() {
        return switch (priority) {
            case 4 -> "#EF4444"; // urgent
            case 3 -> "#F59E0B"; // high
            case 2 -> "#3B82F6"; // medium
            default -> "#9CA3AF"; // low
        };
    }

    @Override
    public String toString() { return title; }
}
