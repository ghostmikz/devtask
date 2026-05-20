package mongolup.server.model;

import java.io.Serializable;

public class Response implements Serializable {
    private static final long serialVersionUID = 1L;

    // requestId == 0 means this is a server-initiated push, not a reply
    private int requestId;
    private boolean success;
    private String message;
    private Object data;

    public Response(int requestId, boolean success, String message, Object data) {
        this.requestId = requestId;
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public static Response ok(int requestId, Object data) {
        return new Response(requestId, true, null, data);
    }

    public static Response fail(int requestId, String message) {
        return new Response(requestId, false, message, null);
    }

    public static Response push(Object data) {
        return new Response(0, true, null, data);
    }

    public int getRequestId()   { return requestId; }
    public boolean isSuccess()  { return success; }
    public String getMessage()  { return message; }
    public Object getData()     { return data; }
}
