package mongolup.server.model;

import java.io.Serializable;

public class Status implements Serializable {
    private static final long serialVersionUID = 1L;

    private int statusId;
    private int projectId;
    private String name;
    private String color;
    private String type;    // todo | in_progress | review | done
    private int sortOrder;

    public Status() {}

    public int getStatusId()            { return statusId; }
    public void setStatusId(int v)      { statusId = v; }

    public int getProjectId()           { return projectId; }
    public void setProjectId(int v)     { projectId = v; }

    public String getName()             { return name; }
    public void setName(String v)       { name = v; }

    public String getColor()            { return color; }
    public void setColor(String v)      { color = v; }

    public String getType()             { return type; }
    public void setType(String v)       { type = v; }

    public int getSortOrder()           { return sortOrder; }
    public void setSortOrder(int v)     { sortOrder = v; }

    @Override
    public String toString() { return name; }
}
