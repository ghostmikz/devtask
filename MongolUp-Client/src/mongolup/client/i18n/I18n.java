package mongolup.client.i18n;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

/**
 * Runtime-switchable internationalization helper.
 * All UI components register a refresh callback via I18n.onLocaleChange().
 */
public class I18n {

    public static final String MN = "mn";
    public static final String EN = "en";

    private static volatile Locale currentLocale = new Locale("mn");
    private static volatile ResourceBundle bundle = load(currentLocale);

    private static final List<Runnable> listeners = new ArrayList<>();

    private I18n() {}

    /** Get a localized string by key. Returns the key itself if not found. */
    public static String t(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return key;
        }
    }

    /** Switch locale at runtime and notify all registered components. */
    public static void setLocale(String lang) {
        currentLocale = new Locale(lang);
        bundle = load(currentLocale);
        List<Runnable> copy;
        synchronized (listeners) { copy = new ArrayList<>(listeners); }
        // run on EDT
        javax.swing.SwingUtilities.invokeLater(() -> copy.forEach(Runnable::run));
    }

    public static String getCurrentLang() {
        return currentLocale.getLanguage();
    }

    /** Register a callback to be invoked on the EDT when locale changes. */
    public static void onLocaleChange(Runnable r) {
        synchronized (listeners) { listeners.add(r); }
    }

    /** Remove a previously registered callback (call from component dispose). */
    public static void removeLocaleListener(Runnable r) {
        synchronized (listeners) { listeners.remove(r); }
    }

    private static ResourceBundle load(Locale locale) {
        // ResourceBundle.getBundle reads .properties as ISO-8859-1 by default.
        // We load manually with UTF-8 so Cyrillic/Mongolian characters render correctly.
        String lang = locale.getLanguage();
        String[] candidates = {
            "mongolup/client/resources/messages_" + lang + ".properties",
            "mongolup/client/resources/messages_mn.properties"
        };
        for (String path : candidates) {
            try (InputStream is = I18n.class.getClassLoader().getResourceAsStream(path)) {
                if (is != null) {
                    return new PropertyResourceBundle(new InputStreamReader(is, StandardCharsets.UTF_8));
                }
            } catch (IOException ignored) {}
        }
        throw new MissingResourceException("No messages bundle found", "messages", lang);
    }
}
