package mongolup.server.model;

import java.io.Serializable;

public class Label implements Serializable {
    private static final long serialVersionUID = 1L;

    private int labelId;
    private int projectId;
    private String name;
    private String color;

    public Label() {}

    public int getLabelId()         { return labelId; }
    public void setLabelId(int v)   { labelId = v; }

    public int getProjectId()       { return projectId; }
    public void setProjectId(int v) { projectId = v; }

    public String getName()         { return name; }
    public void setName(String v)   { name = v; }

    public String getColor()        { return color; }
    public void setColor(String v)  { color = v; }

    @Override
    public String toString() { return name; }
}
