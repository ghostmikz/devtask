package mongolup.server.model;

import java.io.Serializable;

public class UpdateEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type {
        TASK_CREATED, TASK_UPDATED, TASK_STATUS_CHANGED,
        TASK_ASSIGNED, TASK_DELETED, COMMENT_ADDED, PROJECT_CREATED
    }

    private Type type;
    private int projectId;
    private Object payload;

    public UpdateEvent(Type type, int projectId, Object payload) {
        this.type = type;
        this.projectId = projectId;
        this.payload = payload;
    }

    public Type getType()       { return type; }
    public int getProjectId()   { return projectId; }
    public Object getPayload()  { return payload; }
}
