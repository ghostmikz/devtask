package mongolup.client.ui;

import mongolup.client.i18n.I18n;
import mongolup.server.model.Sprint;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class TopbarPanel extends JPanel {

    private static final Color BG       = Color.WHITE;
    private static final Color ACCENT   = new Color(0x1A1A18);
    private static final Color BORDER   = new Color(0xE8E8E5);
    private static final Color TEXT_SEC = new Color(0x888780);
    private static final Color SUCCESS_BG = new Color(0xE1F5EE);
    private static final Color SUCCESS_FG = new Color(0x0F6E56);

    private JLabel  breadcrumbCur;
    private JLabel  sprintBadge;
    private JLabel  sprintDate;
    private JButton addButton;
    private JButton langToggle;
    private JPanel  priorityLegend;  // rebuilt on locale change
    private JPanel  rightPanel;

    private String currentPage = "kanban";
    private final Consumer<String> addAction;     // called with pageId
    private final BiConsumer<String, String> langChangeCallback; // (lang, display)

    public TopbarPanel(Consumer<String> addAction,
                       BiConsumer<String, String> langChangeCallback) {
        this.addAction           = addAction;
        this.langChangeCallback  = langChangeCallback;

        setBackground(BG);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                new EmptyBorder(0, 20, 0, 20)));
        setPreferredSize(new Dimension(0, 56));
        setLayout(new BorderLayout(12, 0));
        buildUI();
        I18n.onLocaleChange(this::refreshText);
    }

    private void buildUI() {
        // LEFT: breadcrumb
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 14));
        left.setBackground(BG);
        JLabel proj = new JLabel("DevTask");
        proj.setForeground(TEXT_SEC);
        proj.setFont(new Font("Arial", Font.PLAIN, 12));
        JLabel sep = new JLabel("/");
        sep.setForeground(TEXT_SEC);
        breadcrumbCur = new JLabel(I18n.t("nav.kanban"));
        breadcrumbCur.setFont(new Font("Arial", Font.BOLD, 12));
        breadcrumbCur.setForeground(ACCENT);

        sprintBadge = new JLabel();
        sprintBadge.setBackground(SUCCESS_BG);
        sprintBadge.setForeground(SUCCESS_FG);
        sprintBadge.setFont(new Font("Arial", Font.BOLD, 11));
        sprintBadge.setBorder(new EmptyBorder(3, 10, 3, 10));
        sprintBadge.setOpaque(true);
        sprintBadge.setVisible(false);   // hidden until a sprint is loaded

        sprintDate = new JLabel();
        sprintDate.setForeground(TEXT_SEC);
        sprintDate.setFont(new Font("Arial", Font.PLAIN, 11));
        sprintDate.setVisible(false);

        left.add(proj);
        left.add(sep);
        left.add(breadcrumbCur);
        left.add(Box.createHorizontalStrut(8));
        left.add(sprintBadge);
        left.add(sprintDate);
        add(left, BorderLayout.WEST);

        // RIGHT: priority legend + lang toggle + add button
        rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 14));
        rightPanel.setBackground(BG);
        JPanel right = rightPanel;

        // Priority legend (stored so it can be rebuilt on locale change)
        priorityLegend = buildPriorityLegend();
        right.add(priorityLegend);
        right.add(Box.createHorizontalStrut(6));

        // Language toggle
        langToggle = new JButton(I18n.getCurrentLang().equals("mn") ? "EN" : "MN");
        langToggle.setFont(new Font("Arial", Font.BOLD, 11));
        langToggle.setForeground(ACCENT);
        langToggle.setBackground(new Color(0xF5F5F3));
        langToggle.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(4, 10, 4, 10)));
        langToggle.setFocusPainted(false);
        langToggle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        langToggle.addActionListener(e -> {
            String newLang = I18n.getCurrentLang().equals("mn") ? "en" : "mn";
            I18n.setLocale(newLang);
            langToggle.setText(newLang.equals("mn") ? "EN" : "MN");
            if (langChangeCallback != null) langChangeCallback.accept(newLang, "");
        });
        right.add(langToggle);

        // Add button
        addButton = new JButton(I18n.t("topbar.add_task"));
        addButton.setBackground(ACCENT);
        addButton.setForeground(Color.WHITE);
        addButton.setFont(new Font("Arial", Font.BOLD, 12));
        addButton.setBorderPainted(false);
        addButton.setFocusPainted(false);
        addButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addButton.setBorder(new EmptyBorder(8, 14, 8, 14));
        addButton.addActionListener(e -> addAction.accept(currentPage));
        right.add(addButton);

        add(rightPanel, BorderLayout.EAST);
    }

    public void setPage(String pageId, String pageLabel) {
        currentPage = pageId;
        breadcrumbCur.setText(pageLabel);
        boolean showSprint = pageId.equals("kanban") || pageId.equals("tasks") || pageId.equals("reports");
        sprintBadge.setVisible(showSprint);
        sprintDate.setVisible(showSprint);
        addButton.setText(addBtnLabel(pageId));
    }

    private String addBtnLabel(String pageId) {
        return switch (pageId) {
            case "reports"  -> I18n.t("topbar.export");
            case "projects" -> I18n.t("topbar.add_project");
            case "profile"  -> I18n.t("topbar.edit");
            default         -> I18n.t("topbar.add_task");
        };
    }

    /** Called by MainFrame when the active sprint for the current project changes. */
    public void setActiveSprint(Sprint sprint) {
        if (sprint != null) {
            sprintBadge.setText(I18n.t("topbar.sprint") + ": " + sprint.getName());
            SimpleDateFormat sdf = new SimpleDateFormat("MMM d");
            String dateRange = (sprint.getStartDate() != null ? sdf.format(sprint.getStartDate()) : "")
                    + " — "
                    + (sprint.getEndDate() != null ? sdf.format(sprint.getEndDate()) : "");
            sprintDate.setText(dateRange);
            sprintBadge.setVisible(true);
            sprintDate.setVisible(true);
        } else {
            sprintBadge.setVisible(false);
            sprintDate.setVisible(false);
        }
    }

    private void refreshText() {
        addButton.setText(addBtnLabel(currentPage));
        langToggle.setText(I18n.getCurrentLang().equals("mn") ? "EN" : "MN");
        // Rebuild priority legend labels
        if (priorityLegend != null && rightPanel != null) {
            rightPanel.remove(priorityLegend);
            priorityLegend = buildPriorityLegend();
            rightPanel.add(priorityLegend, 0);
            rightPanel.revalidate();
            rightPanel.repaint();
        }
    }

    private JPanel buildPriorityLegend() {
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        legend.setBackground(BG);
        legend.add(priorityDot("#EF4444", I18n.t("priority.urgent")));
        legend.add(priorityDot("#F59E0B", I18n.t("priority.high")));
        legend.add(priorityDot("#3B82F6", I18n.t("priority.medium")));
        legend.add(priorityDot("#9CA3AF", I18n.t("priority.low")));
        return legend;
    }

    private JPanel priorityDot(String hex, String label) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        p.setBackground(BG);
        JLabel dot = new JLabel("●");
        dot.setForeground(Color.decode(hex));
        dot.setFont(new Font("Arial", Font.PLAIN, 8));
        JLabel lbl = new JLabel(label);
        lbl.setForeground(TEXT_SEC);
        lbl.setFont(new Font("Arial", Font.PLAIN, 11));
        p.add(dot);
        p.add(lbl);
        return p;
    }
}
