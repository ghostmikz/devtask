package mongolup.client;

import mongolup.server.model.Request;
import mongolup.server.model.Response;
import mongolup.server.model.UpdateEvent;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages the single TCP socket connection to the DevTask server.
 * <p>
 * – send() is blocking (waits up to TIMEOUT_MS for the matching response).
 * – A background ListenerThread routes incoming responses:
 *      requestId > 0  → fulfills a pending CompletableFuture
 *      requestId == 0 → server-initiated push, dispatched via EventBus
 */
public class ServerConnection {
    private static final Logger LOG = Logger.getLogger(ServerConnection.class.getName());
    private static final int TIMEOUT_MS = 15_000;

    private static final ServerConnection INSTANCE = new ServerConnection();

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream  in;
    private volatile boolean connected = false;

    private final ConcurrentHashMap<Integer, CompletableFuture<Response>> pending
            = new ConcurrentHashMap<>();

    private ServerConnection() {}

    public static ServerConnection getInstance() { return INSTANCE; }

    // ── lifecycle ─────────────────────────────────────────────────────────────

    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in  = new ObjectInputStream(socket.getInputStream());
        connected = true;
        Thread t = new Thread(this::listenLoop, "ServerListenerThread");
        t.setDaemon(true);
        t.start();
        LOG.info("Connected to " + host + ":" + port);
    }

    public void disconnect() {
        connected = false;
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }

    public boolean isConnected() { return connected; }

    // ── request/response ──────────────────────────────────────────────────────

    /**
     * Send a request and block until the server replies (or timeout).
     * Safe to call from a background thread or SwingWorker.
     */
    public Response send(String action, Object payload) throws IOException {
        if (!connected) throw new IOException("Not connected");
        String token = AppContext.getInstance().getToken();
        Request req = new Request(action, payload, token);
        CompletableFuture<Response> future = new CompletableFuture<>();
        pending.put(req.getRequestId(), future);
        synchronized (out) {
            out.writeObject(req);
            out.flush();
            out.reset();
        }
        try {
            return future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            pending.remove(req.getRequestId());
            throw new IOException("Request timed out: " + action);
        } catch (Exception e) {
            pending.remove(req.getRequestId());
            throw new IOException("Request failed: " + e.getMessage(), e);
        }
    }

    // ── listener loop ─────────────────────────────────────────────────────────

    private void listenLoop() {
        try {
            while (connected) {
                Response resp = (Response) in.readObject();
                if (resp.getRequestId() == 0) {
                    // server push
                    dispatchPush(resp);
                } else {
                    CompletableFuture<Response> future = pending.remove(resp.getRequestId());
                    if (future != null) future.complete(resp);
                }
            }
        } catch (EOFException | java.net.SocketException e) {
            LOG.info("Server connection closed.");
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Listener error", e);
        } finally {
            connected = false;
            EventBus.publish("CONNECTION_LOST", null);
        }
    }

    private void dispatchPush(Response push) {
        if (!(push.getData() instanceof UpdateEvent event)) return;
        String busEvent = switch (event.getType()) {
            case TASK_CREATED        -> EventBus.TASK_CREATED;
            case TASK_UPDATED        -> EventBus.TASK_UPDATED;
            case TASK_STATUS_CHANGED -> EventBus.TASK_STATUS_CHANGED;
            case TASK_ASSIGNED       -> EventBus.TASK_UPDATED;
            case TASK_DELETED        -> EventBus.TASK_DELETED;
            case COMMENT_ADDED       -> EventBus.COMMENT_ADDED;
            case PROJECT_CREATED     -> EventBus.PROJECT_CREATED;
        };
        EventBus.publish(busEvent, event.getPayload());
    }
}
