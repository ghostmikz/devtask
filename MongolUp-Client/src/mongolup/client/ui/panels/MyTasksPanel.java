package mongolup.client.ui.panels;

import mongolup.client.ServerConnection;
import mongolup.client.i18n.I18n;
import mongolup.server.model.Task;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@SuppressWarnings("unchecked")
public class MyTasksPanel extends JPanel {

    private static final Color BG         = new Color(0xF5F5F3);
    private static final Color CARD       = Color.WHITE;
    private static final Color BORDER     = new Color(0xE8E8E5);
    private static final Color TEXT_PRI   = new Color(0x1A1A18);
    private static final Color TEXT_SEC   = new Color(0x888780);
    private static final Color ACCENT     = new Color(0x1A1A18);
    private static final Color ERROR_FG   = new Color(0xA32D2D);
    private static final Color SECTION_BG = new Color(0xF7F7F5);

    private JPanel      listPanel;
    private JPanel      filterBar;
    private String      activeFilter = "all";

    public MyTasksPanel() {
        setBackground(BG);
        setLayout(new BorderLayout());
        buildUI();
        I18n.onLocaleChange(this::rebuildFilters);
    }

    // ── build ─────────────────────────────────────────────────────────────────

    private void buildUI() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 0));
        wrapper.setBackground(BG);
        wrapper.setBorder(new EmptyBorder(22, 22, 22, 22));

        // ── top: title + filter pills
        JPanel top = new JPanel(new BorderLayout(0, 12));
        top.setBackground(BG);
        top.setBorder(new EmptyBorder(0, 0, 14, 0));

        JLabel title = new JLabel(I18n.t("mytasks.title"));
        title.setFont(new Font("Arial", Font.BOLD, 20));
        title.setForeground(TEXT_PRI);
        top.add(title, BorderLayout.NORTH);

        filterBar = buildFilterBar();
        top.add(filterBar, BorderLayout.SOUTH);

        wrapper.add(top, BorderLayout.NORTH);

        // ── center: scrollable task list
        listPanel = new JPanel();
        listPanel.setBackground(CARD);
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getViewport().setBackground(CARD);

        wrapper.add(scroll, BorderLayout.CENTER);
        add(wrapper, BorderLayout.CENTER);
    }

    private JPanel buildFilterBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        bar.setBackground(BG);
        String[] filters = {"all", "active", "done", "overdue"};
        for (String f : filters) {
            JToggleButton pill = new JToggleButton(filterLabel(f));
            pill.setFont(new Font("Arial", Font.PLAIN, 12));
            pill.setSelected(f.equals(activeFilter));
            stylePill(pill, f.equals(activeFilter));
            pill.addActionListener(e -> {
                activeFilter = f;
                for (Component c : bar.getComponents()) {
                    if (c instanceof JToggleButton tb) stylePill(tb, tb == pill);
                }
                load();
            });
            bar.add(pill);
        }
        return bar;
    }

    private void rebuildFilters() {
        if (filterBar == null) return;
        Component[] pills = filterBar.getComponents();
        String[] filters  = {"all", "active", "done", "overdue"};
        for (int i = 0; i < pills.length && i < filters.length; i++) {
            if (pills[i] instanceof JToggleButton tb)
                tb.setText(filterLabel(filters[i]));
        }
        filterBar.revalidate();
        load();
    }

    // ── load ──────────────────────────────────────────────────────────────────

    public void load() {
        new SwingWorker<List<Task>, Void>() {
            @Override protected List<Task> doInBackground() throws Exception {
                var r = ServerConnection.getInstance().send("GET_MY_TASKS", null);
                return r.isSuccess() ? (List<Task>) r.getData() : List.of();
            }
            @Override protected void done() {
                try { renderTasks(filter(get())); }
                catch (Exception ignored) {}
            }
        }.execute();
    }

    private List<Task> filter(List<Task> all) {
        Date now = new Date();
        return all.stream().filter(t -> switch (activeFilter) {
            case "active"  -> !t.isDone();
            case "done"    -> t.isDone();
            case "overdue" -> !t.isDone() && t.getDueDate() != null && t.getDueDate().before(now);
            default        -> true;
        }).toList();
    }

    // ── render ────────────────────────────────────────────────────────────────

    private void renderTasks(List<Task> tasks) {
        listPanel.removeAll();
        Date now = new Date();

        List<Task> overdueTasks = tasks.stream()
                .filter(t -> !t.isDone() && t.getDueDate() != null && t.getDueDate().before(now))
                .toList();
        List<Task> upcomingTasks = tasks.stream()
                .filter(t -> !t.isDone() && (t.getDueDate() == null || !t.getDueDate().before(now)))
                .toList();
        List<Task> doneTasks = tasks.stream().filter(Task::isDone).toList();

        if (!overdueTasks.isEmpty()) {
            addSectionHeader(I18n.t("mytasks.section.urgent"), new Color(0xFEF2F2), new Color(0xA32D2D));
            overdueTasks.forEach(t -> addTaskRow(t, true));
        }
        if (!upcomingTasks.isEmpty()) {
            addSectionHeader(I18n.t("mytasks.section.week"), SECTION_BG, TEXT_SEC);
            upcomingTasks.forEach(t -> addTaskRow(t, false));
        }
        if (!doneTasks.isEmpty()) {
            addSectionHeader(I18n.t("mytasks.section.done"), SECTION_BG, TEXT_SEC);
            doneTasks.forEach(t -> addTaskRow(t, false));
        }
        if (tasks.isEmpty()) {
            JPanel empty = new JPanel(new GridBagLayout());
            empty.setBackground(CARD);
            empty.setPreferredSize(new Dimension(0, 200));
            JLabel lbl = new JLabel(I18n.t("mytasks.filter." + activeFilter) + " — 0");
            lbl.setForeground(TEXT_SEC);
            lbl.setFont(new Font("Arial", Font.PLAIN, 13));
            empty.add(lbl);
            listPanel.add(empty);
        }

        listPanel.revalidate();
        listPanel.repaint();
    }

    private void addSectionHeader(String text, Color bg, Color fg) {
        JLabel lbl = new JLabel("  " + text.toUpperCase());
        lbl.setFont(new Font("Arial", Font.BOLD, 10));
        lbl.setForeground(fg);
        lbl.setBackground(bg);
        lbl.setOpaque(true);
        lbl.setBorder(new EmptyBorder(8, 10, 8, 10));
        lbl.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        lbl.setAlignmentX(LEFT_ALIGNMENT);
        listPanel.add(lbl);
    }

    private void addTaskRow(Task task, boolean overdue) {
        JPanel row = new JPanel(new BorderLayout(0, 0));
        row.setBackground(CARD);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        row.setAlignmentX(LEFT_ALIGNMENT);

        JPanel inner = new JPanel(new BorderLayout(12, 0));
        inner.setBackground(CARD);
        inner.setBorder(new EmptyBorder(11, 16, 11, 16));

        // ── left: priority dot + title (no checkbox — read-only list)
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setBackground(CARD);

        JLabel dot = new JLabel("●");
        dot.setFont(new Font("Arial", Font.PLAIN, 9));
        dot.setForeground(Color.decode(task.getPriorityColor()));

        JLabel name = new JLabel(task.getTitle());
        name.setFont(new Font("Arial", Font.PLAIN, 13));
        name.setForeground(task.isDone() ? TEXT_SEC : TEXT_PRI);
        if (task.isDone()) {
            name.setText("<html><strike>" + escHtml(task.getTitle()) + "</strike></html>");
        }

        left.add(dot);
        left.add(name);

        // ── right: status badge + due date
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setBackground(CARD);

        if (task.getStatusName() != null) {
            JLabel badge = new JLabel(task.getStatusName());
            badge.setFont(new Font("Arial", Font.PLAIN, 11));
            badge.setForeground(TEXT_SEC);
            badge.setBackground(BG);
            badge.setOpaque(true);
            badge.setBorder(new EmptyBorder(2, 8, 2, 8));
            right.add(badge);
        }

        if (task.getDueDate() != null) {
            String dateStr = new SimpleDateFormat("MM/dd").format(task.getDueDate());
            if (overdue) dateStr += "  " + I18n.t("mytasks.overdue");
            JLabel due = new JLabel(dateStr);
            due.setFont(new Font("Arial", Font.PLAIN, 11));
            due.setForeground(overdue ? ERROR_FG : TEXT_SEC);
            if (overdue) due.setFont(due.getFont().deriveFont(Font.BOLD));
            right.add(due);
        }

        inner.add(left, BorderLayout.WEST);
        inner.add(right, BorderLayout.EAST);

        row.add(inner, BorderLayout.CENTER);
        row.add(makeSep(), BorderLayout.SOUTH);

        listPanel.add(row);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private JSeparator makeSep() {
        JSeparator sep = new JSeparator();
        sep.setForeground(BORDER);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return sep;
    }

    private String filterLabel(String f) {
        return switch (f) {
            case "all"     -> I18n.t("mytasks.filter.all");
            case "active"  -> I18n.t("mytasks.filter.active");
            case "done"    -> I18n.t("mytasks.filter.done");
            case "overdue" -> I18n.t("mytasks.filter.overdue");
            default -> f;
        };
    }

    private void stylePill(JToggleButton b, boolean active) {
        b.setBackground(active ? ACCENT : CARD);
        b.setForeground(active ? Color.WHITE : TEXT_SEC);
        b.setBorderPainted(true);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(active ? ACCENT : BORDER),
                new EmptyBorder(5, 14, 5, 14)));
        b.setFocusPainted(false);
        b.setContentAreaFilled(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private String escHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
