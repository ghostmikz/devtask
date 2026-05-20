package mongolup.client.ui.components;

import javax.swing.*;
import java.awt.*;

/** A JPanel with rounded corners and an optional border. */
public class RoundedPanel extends JPanel {

    private final int radius;
    private Color borderColor;

    public RoundedPanel(int radius) {
        this(radius, null);
    }

    public RoundedPanel(int radius, Color borderColor) {
        this.radius = radius;
        this.borderColor = borderColor;
        setOpaque(false);
    }

    public void setBorderColor(Color c) { this.borderColor = c; repaint(); }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
        if (borderColor != null) {
            g2.setColor(borderColor);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
        }
        g2.dispose();
        super.paintComponent(g);
    }
}
