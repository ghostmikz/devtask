package mongolup.client;

import com.formdev.flatlaf.FlatLightLaf;
import mongolup.client.i18n.I18n;
import mongolup.client.ui.LoginFrame;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

public class Main {
    private static final Logger LOG = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        // 1. Install FlatLaf
        try {
            FlatLightLaf.setup();
            customizeTheme();
        } catch (Exception e) {
            LOG.warning("FlatLaf not available, using default L&F");
        }

        // 2. Load client config
        Properties cfg = new Properties();
        try (InputStream is = Main.class.getClassLoader()
                .getResourceAsStream("client.properties")) {
            if (is != null) cfg.load(is);
        } catch (IOException e) {
            LOG.warning("Could not read client.properties");
        }

        // 3. Apply saved language
        String lang = cfg.getProperty("language", "mn");
        I18n.setLocale(lang);

        // 4. Connect to server
        String host = cfg.getProperty("server.host", "localhost");
        int port;
        try { port = Integer.parseInt(cfg.getProperty("server.port", "9090")); }
        catch (NumberFormatException e) { port = 9090; }

        final String fHost = host;
        final int fPort = port;

        SwingUtilities.invokeLater(() -> {
            try {
                ServerConnection.getInstance().connect(fHost, fPort);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null,
                        I18n.t("error.connection") + "\n" + e.getMessage(),
                        "Connection Error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
            new LoginFrame().setVisible(true);
        });
    }

    private static void customizeTheme() {
        // Match HTML design colors via FlatLaf UIManager overrides
        UIManager.put("Panel.background",               new Color(0xF5F5F3));
        UIManager.put("TextField.background",           new Color(0xFAFAF8));
        UIManager.put("PasswordField.background",       new Color(0xFAFAF8));
        UIManager.put("TextArea.background",            new Color(0xFAFAF8));
        UIManager.put("ComboBox.background",            new Color(0xFAFAF8));
        UIManager.put("TextField.focusedBorderColor",   new Color(0x888888));
        UIManager.put("PasswordField.focusedBorderColor", new Color(0x888888));
        UIManager.put("Button.arc",                     8);
        UIManager.put("Component.arc",                  7);
        UIManager.put("ScrollBar.width",                6);
        UIManager.put("defaultFont",
                new Font("Segoe UI", Font.PLAIN, 13));
    }
}
