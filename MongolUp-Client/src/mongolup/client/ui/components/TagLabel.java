package mongolup.client.ui.components;

import javax.swing.*;
import java.awt.*;

/** Small pill-shaped label for task tags/labels. */
public class TagLabel extends JLabel {

    public TagLabel(String text, Color bg, Color fg) {
        super(text);
        setOpaque(false);
        setForeground(fg);
        setFont(new Font("Segoe UI", Font.BOLD, 10));
        setBorder(BorderFactory.createEmptyBorder(2, 7, 2, 7));
        setBackground(bg); // stored but painted manually
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 5, 5);
        g2.dispose();
        super.paintComponent(g);
    }

    @Override
    public boolean isOpaque() { return false; }
}
