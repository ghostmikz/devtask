package mongolup.client.ui.components;

import javax.swing.*;
import java.awt.*;

/** Circular avatar that shows initials on a colored background. */
public class AvatarLabel extends JLabel {

    private final Color bg;
    private final int size;

    public AvatarLabel(String initials, Color bg, int size) {
        super(initials, CENTER);
        this.bg   = bg;
        this.size = size;
        setOpaque(false);
        setForeground(Color.WHITE);
        setFont(new Font("Segoe UI", Font.BOLD, size / 3));
        setPreferredSize(new Dimension(size, size));
        setMinimumSize(new Dimension(size, size));
        setMaximumSize(new Dimension(size, size));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(bg);
        g2.fillOval(0, 0, size - 1, size - 1);
        g2.dispose();
        super.paintComponent(g);
    }
}
