package mongolup.client.ui;

import mongolup.client.AppContext;
import mongolup.client.i18n.I18n;
import mongolup.server.model.Project;
import mongolup.client.ui.components.AvatarLabel;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SidebarPanel extends JPanel {

    private static final Color BG         = Color.WHITE;
    private static final Color ITEM_HOVER = new Color(0xF5F5F3);
    private static final Color ITEM_ACT   = new Color(0xF0F0EE);
    private static final Color ACCENT_BAR = new Color(0x1A1A18);
    private static final Color TEXT_DIM   = new Color(0x9A9997);
    private static final Color TEXT_FULL  = new Color(0x1A1A18);
    private static final Color SECTION    = new Color(0xBBBBB9);
    private static final Color DOT_ONLINE = new Color(0x4ADE80);

    private static final javax.swing.border.Border BORDER_ACTIVE =
        BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 3, 0, 0, ACCENT_BAR),
            new EmptyBorder(2, 5, 2, 8));
    private static final javax.swing.border.Border BORDER_INACTIVE =
        new EmptyBorder(2, 8, 2, 8);

    private final Consumer<String> pageCallback;
    private final List<JPanel>     navItems        = new ArrayList<>();
    private final List<JPanel>     projItems       = new ArrayList<>();
    private JPanel                 projectListPanel;
    private String                 activeNav       = "kanban";

    public SidebarPanel(Consumer<String> pageCallback) {
        this.pageCallback = pageCallback;
        setBackground(BG);
        setPreferredSize(new Dimension(220, 0));
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(0xE8E8E5)));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        build();
        I18n.onLocaleChange(this::rebuild);
    }

    // ── build ─────────────────────────────────────────────────────────────────

    private void build() {
        removeAll();
        navItems.clear();

        // Logo row
        JPanel logoRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        logoRow.setBackground(BG);
        logoRow.setBorder(new EmptyBorder(18, 16, 14, 16));
        logoRow.setAlignmentX(LEFT_ALIGNMENT);
        logoRow.setMaximumSize(new Dimension(220, 52));
        JLabel dotL = new JLabel("●");
        dotL.setForeground(DOT_ONLINE);
        dotL.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        JLabel logoText = new JLabel("DevTask");
        logoText.setFont(new Font("Segoe UI", Font.BOLD, 17));
        logoText.setForeground(TEXT_FULL);
        logoRow.add(dotL);
        logoRow.add(logoText);
        add(logoRow);
        addSeparator();

        // Main nav
        addSection(I18n.t("nav.main"));
        addNavItem("kanban",   I18n.t("nav.kanban"),   null);
        addNavItem("tasks",    I18n.t("nav.mytasks"),   new Color(0xF59E0B));
        addNavItem("reports",  I18n.t("nav.reports"),  null);
        addNavItem("projects", I18n.t("nav.projects"), null);

        // Project list
        addSection(I18n.t("nav.projects_section"));
        projectListPanel = new JPanel();
        projectListPanel.setBackground(BG);
        projectListPanel.setAlignmentX(LEFT_ALIGNMENT);
        projectListPanel.setLayout(new BoxLayout(projectListPanel, BoxLayout.Y_AXIS));
        add(projectListPanel);

        add(Box.createVerticalGlue());

        // Team section
        addTeamSection();
        addSeparator();

        // Bottom nav
        addNavItem("profile",  I18n.t("nav.profile"),  null);
        addNavItem("settings", I18n.t("nav.settings"), null);
        add(Box.createVerticalStrut(12));

        revalidate();
        repaint();
    }

    private void rebuild() { build(); }

    // ── project list ──────────────────────────────────────────────────────────

    public void setProjects(List<Project> projects) {
        projItems.clear();
        projectListPanel.removeAll();
        for (Project p : projects) {
            JPanel item = buildProjectItem(p);
            projItems.add(item);
            projectListPanel.add(item);
        }
        projectListPanel.revalidate();
        projectListPanel.repaint();
    }

    // ── nav item ──────────────────────────────────────────────────────────────

    private void addNavItem(String pageId, String label, Color badgeColor) {
        boolean isActive = pageId.equals(activeNav);

        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(BG);
        row.setBorder(isActive ? BORDER_ACTIVE : BORDER_INACTIVE);
        row.setMaximumSize(new Dimension(220, 36));
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setOpaque(true);

        JPanel inner = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 7));
        inner.setBackground(isActive ? ITEM_ACT : BG);
        inner.setOpaque(true);
        inner.setBorder(new EmptyBorder(0, 2, 0, 4));

        // Icon
        JLabel iconLbl = new JLabel(loadIcon(pageId, isActive ? TEXT_FULL : TEXT_DIM));
        iconLbl.setPreferredSize(new Dimension(16, 16));

        // Text
        JLabel textLbl = new JLabel(label);
        textLbl.setForeground(isActive ? TEXT_FULL : TEXT_DIM);
        textLbl.setFont(new Font("Segoe UI", isActive ? Font.BOLD : Font.PLAIN, 13));

        inner.add(iconLbl);
        inner.add(textLbl);

        if (badgeColor != null) {
            JLabel badge = new JLabel("●");
            badge.setForeground(badgeColor);
            badge.setFont(new Font("Segoe UI", Font.PLAIN, 8));
            inner.add(badge);
        }

        row.add(inner);
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        row.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                if (!pageId.equals(activeNav)) inner.setBackground(ITEM_HOVER);
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                if (!pageId.equals(activeNav)) inner.setBackground(BG);
            }
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                setActive(pageId);
                pageCallback.accept(pageId);
            }
        });

        row.putClientProperty("pageId", pageId);
        row.putClientProperty("inner",  inner);
        row.putClientProperty("label",  textLbl);
        row.putClientProperty("icon",   iconLbl);
        navItems.add(row);
        add(row);
    }

    private JPanel buildProjectItem(Project p) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 5));
        row.setBackground(BG);
        row.setBorder(new EmptyBorder(0, 14, 0, 8));
        row.setMaximumSize(new Dimension(220, 30));
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel dot = new JLabel("●");
        try { dot.setForeground(Color.decode(p.getColor() != null ? p.getColor() : "#888888")); }
        catch (Exception ex) { dot.setForeground(TEXT_DIM); }
        dot.setFont(new Font("Segoe UI", Font.PLAIN, 9));

        JLabel name = new JLabel(p.getName());
        name.setForeground(TEXT_DIM);
        name.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        row.add(dot);
        row.add(name);
        row.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                AppContext.getInstance().setCurrentProject(p);
                pageCallback.accept("kanban");
            }
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { row.setBackground(ITEM_HOVER); }
            @Override public void mouseExited (java.awt.event.MouseEvent e) { row.setBackground(BG); }
        });
        return row;
    }

    // ── active state ──────────────────────────────────────────────────────────

    private void setActive(String pageId) {
        activeNav = pageId;
        for (JPanel row : navItems) {
            String  pid   = (String) row.getClientProperty("pageId");
            JPanel  inner = (JPanel) row.getClientProperty("inner");
            JLabel  lbl   = (JLabel) row.getClientProperty("label");
            JLabel  ico   = (JLabel) row.getClientProperty("icon");
            boolean act   = pid != null && pid.equals(pageId);

            row.setBorder(act ? BORDER_ACTIVE : BORDER_INACTIVE);
            if (inner != null) inner.setBackground(act ? ITEM_ACT : BG);
            if (lbl   != null) {
                lbl.setForeground(act ? TEXT_FULL : TEXT_DIM);
                lbl.setFont(new Font("Segoe UI", act ? Font.BOLD : Font.PLAIN, 13));
            }
            if (ico != null && pid != null) {
                ico.setIcon(loadIcon(pid, act ? TEXT_FULL : TEXT_DIM));
            }
        }
        revalidate();
        repaint();
    }

    public void setActivePage(String pageId) { setActive(pageId); }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void addTeamSection() {
        addSection(I18n.t("nav.team"));
        AppContext ctx = AppContext.getInstance();
        if (ctx.getCurrentUser() != null) addMember(ctx.getCurrentUser());
    }

    private void addMember(mongolup.server.model.User u) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        row.setBackground(BG);
        row.setMaximumSize(new Dimension(220, 38));
        row.setAlignmentX(LEFT_ALIGNMENT);
        Color aColor = avatarColor(u.getUserId());
        AvatarLabel av = new AvatarLabel(u.getInitials(), aColor, 26);
        JLabel name = new JLabel(u.getFullName() != null ? u.getFullName() : u.getUsername());
        name.setForeground(TEXT_DIM);
        name.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        row.add(av);
        row.add(name);
        add(row);
    }

    private void addSection(String text) {
        JLabel lbl = new JLabel(text.toUpperCase());
        lbl.setForeground(SECTION);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lbl.setBorder(new EmptyBorder(10, 16, 4, 14));
        lbl.setAlignmentX(LEFT_ALIGNMENT);
        lbl.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        add(lbl);
    }

    private void addSeparator() {
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(0xE8E8E5));
        sep.setBackground(BG);
        sep.setMaximumSize(new Dimension(220, 1));
        sep.setAlignmentX(LEFT_ALIGNMENT);
        add(sep);
    }

    // ── icon loading ──────────────────────────────────────────────────────────

    private Icon loadIcon(String pageId, Color tint) {
        String path = "mongolup/client/resources/icons/" + pageId + ".png";
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is != null) {
                BufferedImage raw = ImageIO.read(is);
                // Tint the icon to match active/inactive color
                BufferedImage tinted = tintImage(raw, tint);
                return new ImageIcon(tinted.getScaledInstance(16, 16, Image.SCALE_SMOOTH));
            }
        } catch (IOException ignored) {}
        return generateFallbackIcon(pageId, tint);
    }

    /** Recolour a grayscale/RGBA icon to the given tint color. */
    private BufferedImage tintImage(BufferedImage src, Color tint) {
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                int argb  = src.getRGB(x, y);
                int alpha = (argb >> 24) & 0xFF;
                out.setRGB(x, y, (alpha << 24) | (tint.getRed() << 16)
                        | (tint.getGreen() << 8) | tint.getBlue());
            }
        }
        return out;
    }

    /** Simple fallback icon drawn with Java2D when PNG isn't available. */
    private Icon generateFallbackIcon(String pageId, Color color) {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(color);
        switch (pageId) {
            case "kanban"   -> { g.fillRect(1,1,6,6); g.fillRect(9,1,6,6); g.fillRect(1,9,6,6); g.fillRect(9,9,6,6); }
            case "tasks"    -> { for (int y : new int[]{2,6,10}) { g.fillRect(1,y,4,3); g.fillRect(6,y+1,9,1); } }
            case "reports"  -> { g.fillRect(1,10,3,5); g.fillRect(6,6,3,9); g.fillRect(12,2,3,13); }
            case "projects" -> { g.fillRect(1,5,7,10); g.fillRect(9,1,6,14); }
            case "profile"  -> { g.fillOval(5,1,6,6); g.fillOval(2,9,12,6); }
            case "settings" -> { g.fillOval(4,4,8,8); g.setColor(getBackground()); g.fillOval(6,6,4,4); }
            default         -> g.fillRoundRect(2,2,12,12,4,4);
        }
        g.dispose();
        return new ImageIcon(img);
    }

    private Color avatarColor(int id) {
        Color[] palette = { new Color(0x0F6E56), new Color(0x7C3AED),
                            new Color(0x2563EB), new Color(0xD97706) };
        return palette[Math.abs(id) % palette.length];
    }
}
