package mongolup.client.ui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Shows transient floating toast notifications anchored to the bottom-right
 * of the currently active window. Auto-dismisses after 2.5 seconds.
 */
public class ToastManager {

    private static final Color SUCCESS = new Color(0x0F6E56);
    private static final Color ERROR   = new Color(0xA32D2D);
    private static final Color INFO    = new Color(0x1A1A18);

    private ToastManager() {}

    public static void success(String msg) { show("✓  " + msg, SUCCESS); }
    public static void error(String msg)   { show("✕  " + msg, ERROR);   }
    public static void info(String msg)    { show("ℹ  " + msg, INFO);    }

    private static void show(String msg, Color bg) {
        SwingUtilities.invokeLater(() -> {
            Window active = KeyboardFocusManager
                    .getCurrentKeyboardFocusManager().getActiveWindow();
            if (active == null) return;

            JWindow toast = new JWindow(active);
            toast.setAlwaysOnTop(true);

            JLabel lbl = new JLabel(msg);
            lbl.setFont(new Font("Arial", Font.PLAIN, 13));
            lbl.setForeground(Color.WHITE);
            lbl.setBackground(bg);
            lbl.setOpaque(true);
            lbl.setBorder(new EmptyBorder(12, 20, 12, 20));

            // Rounded wrapper panel
            JPanel wrap = new JPanel(new BorderLayout()) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                        RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(bg);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                    g2.dispose();
                }
            };
            wrap.setOpaque(false);
            wrap.add(lbl);
            toast.getContentPane().add(wrap);
            toast.pack();

            // Position: bottom-right of active window, 20px inset
            Rectangle wb = active.getBounds();
            toast.setLocation(
                    wb.x + wb.width  - toast.getWidth()  - 24,
                    wb.y + wb.height - toast.getHeight() - 56);
            toast.setVisible(true);

            Timer timer = new Timer(2500, e -> toast.dispose());
            timer.setRepeats(false);
            timer.start();
        });
    }
}
