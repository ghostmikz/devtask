package mongolup.client.ui.panels.kanban;

import mongolup.client.i18n.I18n;
import mongolup.server.model.Task;
import mongolup.client.ui.components.AvatarLabel;
import mongolup.client.ui.components.TagLabel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A Kanban card that is both draggable (DragSource) and displays task info.
 */
public class TaskCard extends JPanel implements DragGestureListener, DragSourceListener, Transferable {

    public static final DataFlavor TASK_FLAVOR =
            new DataFlavor(Task.class, "Task");

    private static final DataFlavor[] FLAVORS = {TASK_FLAVOR};

    private static final Color CARD_BG   = Color.WHITE;
    private static final Color BORDER    = new Color(0xE8E8E5);
    private static final Color HOVER_BDR = new Color(0xC8C8C5);
    private static final Color TEXT_PRI  = new Color(0x1A1A18);
    private static final Color TEXT_SEC  = new Color(0x888780);

    private static final Color[] AVATAR_PAL = {
        new Color(0x0F6E56), new Color(0x7C3AED),
        new Color(0x2563EB), new Color(0xD97706),
        new Color(0xDB2777), new Color(0x059669)
    };

    private final Task task;
    private final Runnable onClick;
    private final Runnable onMarkDone; // nullable — null means checkbox hidden

    private static final int RADIUS = 10;
    private Color currentBorder = BORDER;

    public TaskCard(Task task, Runnable onClick) {
        this(task, onClick, null);
    }

    public TaskCard(Task task, Runnable onClick, Runnable onMarkDone) {
        this.task       = task;
        this.onClick    = onClick;
        this.onMarkDone = onMarkDone;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(task.isDone() ? new Color(0xFAFAF8) : CARD_BG);
        setBorder(new EmptyBorder(11, 12, 11, 12));
        setOpaque(false);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        buildContent();
        setupDnd();
        setupHover();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), RADIUS, RADIUS);
        g2.setColor(currentBorder);
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, RADIUS, RADIUS);
        g2.dispose();
        super.paintComponent(g);
    }

    public Task getTask() { return task; }

    private void buildContent() {
        // Title row — no checkbox (mark done by dragging to done column)
        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        titleRow.setOpaque(false);
        titleRow.setAlignmentX(LEFT_ALIGNMENT);

        String titleHtml = task.isDone()
                ? "<html><body style='width:160px'><s>" + escapeHtml(task.getTitle()) + "</s></body></html>"
                : "<html><body style='width:160px'>"    + escapeHtml(task.getTitle()) + "</body></html>";
        JLabel title = new JLabel(titleHtml);
        title.setFont(new Font("Arial", Font.PLAIN, 12));
        title.setForeground(task.isDone() ? TEXT_SEC : TEXT_PRI);
        titleRow.add(title);
        add(titleRow);
        add(Box.createVerticalStrut(8));

        // Status/label tag
        if (task.getStatusName() != null) {
            JPanel tags = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            tags.setBackground(getBackground());
            tags.setAlignmentX(LEFT_ALIGNMENT);
            Color tagBg = parseHex(task.getStatusColor(), new Color(0xE8E8E5));
            Color tagFg = TEXT_PRI;
            tags.add(new TagLabel(task.getStatusName(), tagBg, tagFg));
            add(tags);
            add(Box.createVerticalStrut(8));
        }

        // Label chips (white text on colored bg)
        String lcsv = task.getLabelsCsv();
        if (lcsv != null && !lcsv.isBlank()) {
            JPanel labelRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            labelRow.setOpaque(false);
            labelRow.setAlignmentX(LEFT_ALIGNMENT);
            for (String entry : lcsv.split("\\|")) {
                String[] kv = entry.split(":", 2);
                if (kv.length < 2) continue;
                String lname = kv[0];
                Color  lbg   = parseHex(kv[1], new Color(0xE8E8E5));
                // inline chip — rounded pill painted manually
                JLabel chip = new JLabel(lname) {
                    @Override protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(lbg);
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                        g2.dispose();
                        super.paintComponent(g);
                    }
                };
                chip.setFont(new Font("Arial", Font.BOLD, 10));
                chip.setForeground(Color.WHITE);
                chip.setBorder(new EmptyBorder(2, 7, 2, 7));
                chip.setOpaque(false);
                labelRow.add(chip);
            }
            add(labelRow);
            add(Box.createVerticalStrut(6));
        }

        // Assignee avatars
        String csv = task.getAssigneesCsv();
        if (csv != null && !csv.isBlank()) {
            String[] parts = csv.split("\\|");
            JPanel avatarRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
            avatarRow.setOpaque(false);
            avatarRow.setAlignmentX(LEFT_ALIGNMENT);
            int shown = Math.min(parts.length, 4);
            for (int i = 0; i < shown; i++) {
                String[] kv = parts[i].split(":", 2);
                int uid = 0;
                String name = parts[i];
                if (kv.length == 2) {
                    try { uid = Integer.parseInt(kv[0]); } catch (NumberFormatException ignored) {}
                    name = kv[1];
                }
                Color col = AVATAR_PAL[Math.abs(uid > 0 ? uid : i) % AVATAR_PAL.length];
                avatarRow.add(new AvatarLabel(computeInitials(name), col, 20));
            }
            if (parts.length > 4) {
                JLabel more = new JLabel("+" + (parts.length - 4));
                more.setFont(new Font("Arial", Font.PLAIN, 10));
                more.setForeground(TEXT_SEC);
                avatarRow.add(more);
            }
            add(avatarRow);
            add(Box.createVerticalStrut(6));
        }

        // Footer: points dot + weight
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(getBackground());
        footer.setAlignmentX(LEFT_ALIGNMENT);

        JPanel pts = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        pts.setBackground(getBackground());
        JLabel dot = new JLabel("●");
        dot.setForeground(Color.decode(task.getPriorityColor()));
        dot.setFont(new Font("Arial", Font.PLAIN, 9));
        JLabel wt = new JLabel(task.getWeight() + " " + I18n.t("kanban.points"));
        wt.setFont(new Font("Arial", Font.PLAIN, 11));
        wt.setForeground(TEXT_SEC);
        pts.add(dot);
        pts.add(wt);
        footer.add(pts, BorderLayout.WEST);

        // Due date badge (right side of footer)
        if (task.getDueDate() != null) {
            boolean overdue = !task.isDone() && task.getDueDate().before(new Date());
            String dateStr = new SimpleDateFormat("MM/dd").format(task.getDueDate());
            JLabel dueLbl = new JLabel(overdue ? "⚠ " + dateStr : dateStr);
            dueLbl.setFont(new Font("Arial", Font.PLAIN, 10));
            dueLbl.setForeground(overdue ? new Color(0xA32D2D) : TEXT_SEC);
            footer.add(dueLbl, BorderLayout.EAST);
        }

        add(footer);
    }

    private void setupDnd() {
        DragSource ds = DragSource.getDefaultDragSource();
        ds.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_MOVE, this);
    }

    private void setupHover() {
        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { currentBorder = HOVER_BDR; repaint(); }
            @Override public void mouseExited (MouseEvent e) { currentBorder = BORDER;    repaint(); }
            @Override public void mouseClicked(MouseEvent e) {
                if (onClick != null) onClick.run();
            }
        });
    }

    // ── Transferable ─────────────────────────────────────────────────────────

    @Override public DataFlavor[] getTransferDataFlavors() { return FLAVORS; }
    @Override public boolean isDataFlavorSupported(DataFlavor f) { return TASK_FLAVOR.equals(f); }
    @Override public Object getTransferData(DataFlavor f) { return task; }

    // ── DragGestureListener ───────────────────────────────────────────────────

    @Override
    public void dragGestureRecognized(DragGestureEvent dge) {
        dge.startDrag(DragSource.DefaultMoveDrop, this, this);
    }

    // ── DragSourceListener (no-ops mostly) ───────────────────────────────────

    @Override public void dragEnter(DragSourceDragEvent e) {}
    @Override public void dragOver(DragSourceDragEvent e)  {}
    @Override public void dropActionChanged(DragSourceDragEvent e) {}
    @Override public void dragExit(DragSourceEvent e) {}
    @Override public void dragDropEnd(DragSourceDropEvent e) {}

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private Color parseHex(String hex, Color fallback) {
        try { return hex != null ? Color.decode(hex) : fallback; }
        catch (NumberFormatException e) { return fallback; }
    }

    /** "John Doe" → "JD",  "alice" → "A" */
    private String computeInitials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] words = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) sb.appendCodePoint(w.codePointAt(0));
        }
        return sb.toString().toUpperCase();
    }
}
