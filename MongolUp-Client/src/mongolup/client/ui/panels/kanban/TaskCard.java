package mongolup.client.ui.panels.kanban;

import mongolup.client.i18n.I18n;
import mongolup.server.model.Task;
import mongolup.client.ui.components.TagLabel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.event.*;
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
        // Title row: optional done checkbox + text
        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        titleRow.setOpaque(false);
        titleRow.setAlignmentX(LEFT_ALIGNMENT);

        if (onMarkDone != null) {
            JCheckBox check = new JCheckBox();
            check.setSelected(task.isDone());
            check.setOpaque(false);
            check.setFocusPainted(false);
            check.setMargin(new Insets(0, 0, 0, 0));
            check.addActionListener(e -> {
                if (!task.isDone() && onMarkDone != null) {
                    check.setEnabled(false);
                    Color origBg = getBackground();
                    javax.swing.Timer flash = new javax.swing.Timer(90, null);
                    int[] tick = {0};
                    flash.addActionListener(fe -> {
                        tick[0]++;
                        setBackground(tick[0] % 2 == 0 ? origBg : new Color(0xD1FAE5));
                        repaint();
                        if (tick[0] >= 4) {
                            flash.stop();
                            setBackground(origBg);
                            onMarkDone.run();
                        }
                    });
                    flash.start();
                } else {
                    check.setSelected(true); // already done; use drag to move back
                }
            });
            titleRow.add(check);
        }

        JLabel title = new JLabel("<html><body style='width:140px'>" +
                escapeHtml(task.getTitle()) + "</body></html>");
        title.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        title.setForeground(task.isDone() ? TEXT_SEC : TEXT_PRI);
        if (task.isDone()) {
            title.setText("<html><body style='width:140px'><s>" +
                    escapeHtml(task.getTitle()) + "</s></body></html>");
        }
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

        // Footer: points dot + weight
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(getBackground());
        footer.setAlignmentX(LEFT_ALIGNMENT);

        JPanel pts = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        pts.setBackground(getBackground());
        JLabel dot = new JLabel("●");
        dot.setForeground(Color.decode(task.getPriorityColor()));
        dot.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        JLabel wt = new JLabel(task.getWeight() + " " + I18n.t("kanban.points"));
        wt.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        wt.setForeground(TEXT_SEC);
        pts.add(dot);
        pts.add(wt);
        footer.add(pts, BorderLayout.WEST);

        // Due date badge (right side of footer)
        if (task.getDueDate() != null) {
            boolean overdue = !task.isDone() && task.getDueDate().before(new Date());
            String dateStr = new SimpleDateFormat("MM/dd").format(task.getDueDate());
            JLabel dueLbl = new JLabel(overdue ? "⚠ " + dateStr : dateStr);
            dueLbl.setFont(new Font("Segoe UI", Font.PLAIN, 10));
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
                // Don't open dialog when the click landed on the checkbox
                Component src = SwingUtilities.getDeepestComponentAt(TaskCard.this, e.getX(), e.getY());
                if (src instanceof JCheckBox) return;
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
}
