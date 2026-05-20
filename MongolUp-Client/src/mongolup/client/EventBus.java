package mongolup.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Simple publish-subscribe bus for server push events.
 * Listeners are always invoked on the Swing EDT.
 */
public class EventBus {

    private static final Map<String, List<Consumer<Object>>> subs = new HashMap<>();

    private EventBus() {}

    public static synchronized void subscribe(String event, Consumer<Object> listener) {
        subs.computeIfAbsent(event, k -> new ArrayList<>()).add(listener);
    }

    public static synchronized void unsubscribe(String event, Consumer<Object> listener) {
        List<Consumer<Object>> list = subs.get(event);
        if (list != null) list.remove(listener);
    }

    public static void publish(String event, Object data) {
        List<Consumer<Object>> copy;
        synchronized (EventBus.class) {
            List<Consumer<Object>> list = subs.get(event);
            if (list == null || list.isEmpty()) return;
            copy = new ArrayList<>(list);
        }
        javax.swing.SwingUtilities.invokeLater(() -> copy.forEach(c -> c.accept(data)));
    }

    // ── well-known event names ────────────────────────────────────────────────
    public static final String TASK_CREATED       = "TASK_CREATED";
    public static final String TASK_UPDATED       = "TASK_UPDATED";
    public static final String TASK_STATUS_CHANGED= "TASK_STATUS_CHANGED";
    public static final String COMMENT_ADDED      = "COMMENT_ADDED";
    public static final String PROJECT_CREATED    = "PROJECT_CREATED";
}
