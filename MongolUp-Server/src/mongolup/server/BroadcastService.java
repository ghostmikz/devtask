package mongolup.server;

import mongolup.server.model.Response;
import mongolup.server.model.UpdateEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Singleton that holds all connected ClientHandlers and broadcasts events.
 */
public class BroadcastService {
    private static final Logger LOG = Logger.getLogger(BroadcastService.class.getName());

    private static final BroadcastService INSTANCE = new BroadcastService();
    private final List<ClientHandler> clients = new ArrayList<>();

    private BroadcastService() {}

    public static BroadcastService getInstance() { return INSTANCE; }

    public synchronized void register(ClientHandler handler) {
        clients.add(handler);
        LOG.info("Client registered. Total: " + clients.size());
    }

    public synchronized void unregister(ClientHandler handler) {
        clients.remove(handler);
        LOG.info("Client disconnected. Total: " + clients.size());
    }

    /** Broadcast an update to all clients except the originator. */
    public synchronized void broadcast(UpdateEvent event, ClientHandler origin) {
        Response push = Response.push(event);
        List<ClientHandler> copy = new ArrayList<>(clients);
        for (ClientHandler ch : copy) {
            if (ch != origin) {
                ch.sendPush(push);
            }
        }
    }

    /** Broadcast to ALL clients including originator (e.g. project created). */
    public synchronized void broadcastAll(UpdateEvent event) {
        Response push = Response.push(event);
        for (ClientHandler ch : new ArrayList<>(clients)) {
            ch.sendPush(push);
        }
    }
}
