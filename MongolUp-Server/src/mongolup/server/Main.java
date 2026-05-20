package mongolup.server;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

public class Main {
    private static final Logger LOG = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) throws Exception {
        // Load MySQL driver explicitly (needed in some environments)
        Class.forName("com.mysql.cj.jdbc.Driver");

        int port = 9090;
        try (InputStream is = Main.class.getClassLoader()
                .getResourceAsStream("server.properties")) {
            if (is != null) {
                Properties p = new Properties();
                p.load(is);
                port = Integer.parseInt(p.getProperty("server.port", "9090"));
            }
        } catch (IOException | NumberFormatException e) {
            LOG.warning("Could not read server.properties, using default port 9090");
        }

        new Server(port).start();
    }
}
