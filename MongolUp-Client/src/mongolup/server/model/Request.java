package mongolup.server.model;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

public class Request implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final AtomicInteger counter = new AtomicInteger(0);

    private final int requestId;
    private String action;
    private Object payload;
    private String token;

    public Request(String action, Object payload, String token) {
        this.requestId = counter.incrementAndGet();
        this.action = action;
        this.payload = payload;
        this.token = token;
    }

    public int getRequestId()       { return requestId; }
    public String getAction()       { return action; }
    public Object getPayload()      { return payload; }
    public String getToken()        { return token; }
}
