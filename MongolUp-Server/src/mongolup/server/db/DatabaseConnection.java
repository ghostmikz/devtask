package mongolup.server.db;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnection {

    private static final Properties cfg = new Properties();

    static {
        try (InputStream is = DatabaseConnection.class
                .getClassLoader().getResourceAsStream("server.properties")) {
            if (is != null) cfg.load(is);
        } catch (IOException e) {
            // defaults below will be used
        }
    }

    private static final String URL =
            cfg.getProperty("db.url",
                    "jdbc:mysql://localhost:3306/mongolUpDb" +
                    "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&characterEncoding=utf8");
    private static final String USER = cfg.getProperty("db.user", "root");
    private static final String PASS = cfg.getProperty("db.password", "1234");

    private DatabaseConnection() {}

    /**
     * Returns a new connection each call.
     * Callers are responsible for closing it (try-with-resources).
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}
