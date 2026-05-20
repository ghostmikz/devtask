package mongolup.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class Server {
    private static final Logger LOG = Logger.getLogger(Server.class.getName());

    private final int port;
    private final ExecutorService pool = Executors.newCachedThreadPool();
    private volatile boolean running = true;

    public Server(int port) {
        this.port = port;
    }

    public void start() {
        LOG.info("DevTask Server starting on port " + port);
        try (ServerSocket ss = new ServerSocket(port)) {
            LOG.info("Server ready. Waiting for connections...");
            while (running) {
                Socket client = ss.accept();
                LOG.info("New connection: " + client.getRemoteSocketAddress());
                pool.submit(new ClientHandler(client));
            }
        } catch (IOException e) {
            if (running) LOG.severe("Server error: " + e.getMessage());
        } finally {
            pool.shutdown();
            LOG.info("Server stopped.");
        }
    }

    public void stop() {
        running = false;
    }
}
