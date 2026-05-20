package mongolup.client.ui.panels;

import mongolup.client.AppContext;
import mongolup.client.EventBus;
import mongolup.client.ServerConnection;
import mongolup.client.i18n.I18n;
import mongolup.client.ui.components.ToastManager;
import mongolup.server.model.*;
import mongolup.client.ui.panels.kanban.KanbanColumn;
import mongolup.client.ui.panels.kanban.TaskDetailDialog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.Dialog;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@SuppressWarnings("unchecked")
public class KanbanPanel extends JPanel {

    private static final Color BG       = new Color(0xF5F5F3);
    private static final Color TEXT     = new Color(0x1A1A18);
    private static final Color TEXT_SEC = new Color(0x888780);

    private JPanel  columnsArea;
    private JLabel  projectNameLabel;
    private JLabel  projectDotLabel;
    private final List<KanbanColumn> columns = new ArrayList<>();
    private boolean loaded = false;

    public KanbanPanel() {
        setBackground(BG);
        setLayout(new BorderLayout());
        buildUI();
        subscribeToEvents();
    }

    private void buildUI() {
        // ── Project header bar ────────────────────────────────────────────
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 10));
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xE8E8E5)),
            new EmptyBorder(0, 18, 0, 18)));

        projectDotLabel = new JLabel("●");
        projectDotLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        projectDotLabel.setForeground(new Color(0x4ADE80));

        projectNameLabel = new JLabel(I18n.t("nav.kanban"));
        projectNameLabel.setFont(new Font("Arial", Font.BOLD, 14));
        projectNameLabel.setForeground(TEXT);

        JLabel hint = new JLabel("— " + I18n.t("nav.kanban"));
        hint.setFont(new Font("Arial", Font.PLAIN, 12));
        hint.setForeground(TEXT_SEC);

        header.add(projectDotLabel);
        header.add(projectNameLabel);
        add(header, BorderLayout.NORTH);

        // ── Board columns ─────────────────────────────────────────────────
        columnsArea = new JPanel(new GridLayout(1, 0, 12, 0));
        columnsArea.setBackground(BG);
        columnsArea.setBorder(new EmptyBorder(18, 18, 18, 18));

        JScrollPane scroll = new JScrollPane(columnsArea);
        scroll.setBorder(null);
        scroll.setBackground(BG);
        scroll.getViewport().setBackground(BG);
        add(scroll, BorderLayout.CENTER);
    }

    private void updateHeader() {
        Project p = AppContext.getInstance().getCurrentProject();
        if (p == null) return;
        projectNameLabel.setText(p.getName());
        try {
            projectDotLabel.setForeground(
                p.getColor() != null ? Color.decode(p.getColor()) : new Color(0x4ADE80));
        } catch (Exception ignored) {}
    }

    public void load() {
        Project project = AppContext.getInstance().getCurrentProject();
        if (project == null) return;
        updateHeader();

        new SwingWorker<LoadResult, Void>() {
            @Override
            protected LoadResult doInBackground() throws Exception {
                var sr = ServerConnection.getInstance().send("GET_STATUSES", project.getProjectId());
                var tr = ServerConnection.getInstance().send("GET_TASKS",    project.getProjectId());
                List<Status> statuses = sr.isSuccess() ? (List<Status>) sr.getData() : List.of();
                List<Task>   tasks    = tr.isSuccess() ? (List<Task>)   tr.getData() : List.of();
                return new LoadResult(statuses, tasks);
            }
            @Override
            protected void done() {
                try {
                    LoadResult lr = get();
                    buildColumns(lr.statuses, lr.tasks);
                    loaded = true;
                } catch (Exception e) {
                    showError("Could not load board: " + e.getMessage());
                }
            }
        }.execute();
    }

    // ── new task dialog ───────────────────────────────────────────────────────

    public void showNewTaskDialog(Integer preselectedStatusId) {
        Project project = AppContext.getInstance().getCurrentProject();
        if (project == null) {
            JOptionPane.showMessageDialog(this, I18n.t("error.no_project"),
                    "DevTask", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dlg = new JDialog(owner, I18n.t("task.new.title"),
                Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setSize(460, 560);
        dlg.setLocationRelativeTo(owner);
        dlg.setResizable(false);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        root.setBorder(new EmptyBorder(24, 28, 20, 28));

        // Header
        JLabel header = new JLabel(I18n.t("task.new.title"));
        header.setFont(new Font("Arial", Font.BOLD, 16));
        header.setForeground(new Color(0x1A1A18));
        JLabel projHint = new JLabel("  " + project.getName());
        projHint.setFont(new Font("Arial", Font.PLAIN, 12));
        projHint.setForeground(new Color(0x888780));
        JPanel headerRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        headerRow.setBackground(Color.WHITE);
        headerRow.setBorder(new EmptyBorder(0, 0, 18, 0));
        headerRow.add(header);
        headerRow.add(projHint);
        root.add(headerRow, BorderLayout.NORTH);

        // Form
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL; gc.weightx = 1;
        gc.gridx = 0; gc.insets = new Insets(0, 0, 8, 0);

        JTextField titleField = styledInput();
        JTextArea descField = new JTextArea(3, 0);
        descField.setLineWrap(true); descField.setWrapStyleWord(true);
        descField.setFont(new Font("Arial", Font.PLAIN, 13));
        descField.setBackground(new Color(0xFAFAF8));
        descField.setBorder(new EmptyBorder(8, 10, 8, 10));
        JScrollPane descScroll = new JScrollPane(descField);
        descScroll.setBorder(BorderFactory.createLineBorder(new Color(0xE8E8E5)));
        descScroll.getViewport().setBackground(new Color(0xFAFAF8));
        descScroll.setPreferredSize(new Dimension(0, 80));

        String[] priorities = {I18n.t("priority.low"), I18n.t("priority.medium"),
                               I18n.t("priority.high"), I18n.t("priority.urgent")};
        JComboBox<String> prioBox = new JComboBox<>(priorities);
        prioBox.setSelectedIndex(1);
        prioBox.setBackground(new Color(0xFAFAF8));
        prioBox.setFont(new Font("Arial", Font.PLAIN, 13));

        SpinnerNumberModel weightModel = new SpinnerNumberModel(3, 1, 13, 1);
        JSpinner weightSpinner = new JSpinner(weightModel);
        weightSpinner.setFont(new Font("Arial", Font.PLAIN, 13));
        weightSpinner.setBackground(new Color(0xFAFAF8));
        ((JSpinner.DefaultEditor) weightSpinner.getEditor()).getTextField().setBackground(new Color(0xFAFAF8));

        SpinnerDateModel dateModel = new SpinnerDateModel();
        JSpinner dueDateSpinner = new JSpinner(dateModel);
        JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(dueDateSpinner, "yyyy-MM-dd HH:mm");
        dueDateSpinner.setEditor(dateEditor);
        dateEditor.getTextField().setBackground(new Color(0xFAFAF8));
        dateEditor.getTextField().setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE8E8E5)),
                new EmptyBorder(6, 8, 6, 8)));
        dueDateSpinner.setBorder(null);
        dueDateSpinner.setFont(new Font("Arial", Font.PLAIN, 13));
        // "No due date" checkbox
        JCheckBox noDueDate = new JCheckBox(I18n.t("task.no_due_date"), true);
        noDueDate.setBackground(Color.WHITE);
        noDueDate.setFont(new Font("Arial", Font.PLAIN, 12));
        noDueDate.addActionListener(e -> dueDateSpinner.setEnabled(!noDueDate.isSelected()));
        dueDateSpinner.setEnabled(false);

        String[] colNames = columns.stream()
                .map(c -> c.getStatus().getName()).toArray(String[]::new);
        JComboBox<String> colBox = new JComboBox<>(colNames);
        colBox.setBackground(new Color(0xFAFAF8));
        colBox.setFont(new Font("Arial", Font.PLAIN, 13));
        if (preselectedStatusId != null) {
            for (int i = 0; i < columns.size(); i++) {
                if (columns.get(i).getStatus().getStatusId() == preselectedStatusId) {
                    colBox.setSelectedIndex(i); break;
                }
            }
        }

        gc.gridy = 0; form.add(inputLabel(I18n.t("task.title")), gc);
        gc.gridy = 1; form.add(titleField, gc);
        gc.gridy = 2; form.add(inputLabel(I18n.t("task.description")), gc);
        gc.gridy = 3; form.add(descScroll, gc);
        gc.gridy = 4; form.add(inputLabel(I18n.t("task.priority")), gc);
        gc.gridy = 5; form.add(prioBox, gc);
        gc.gridy = 6; form.add(inputLabel(I18n.t("task.due_date")), gc);
        gc.gridy = 7; form.add(noDueDate, gc);
        gc.insets = new Insets(0, 0, 8, 0);
        gc.gridy = 8; form.add(dueDateSpinner, gc);
        gc.gridy = 9; form.add(inputLabel(I18n.t("task.points")), gc);
        gc.gridy = 10; form.add(weightSpinner, gc);
        gc.gridy = 11; form.add(inputLabel(I18n.t("kanban.column")), gc);
        gc.gridy = 12; form.add(colBox, gc);

        // Attachment section
        final java.util.List<Attachment> pendingAttachments = new java.util.ArrayList<>();
        JPanel attachChips = new JPanel();
        attachChips.setLayout(new BoxLayout(attachChips, BoxLayout.Y_AXIS));
        attachChips.setOpaque(false);

        JLabel chooseFile = new JLabel(I18n.t("task.add_attachment"));
        chooseFile.setForeground(new Color(0x888780));
        chooseFile.setFont(new Font("Arial", Font.PLAIN, 12));
        chooseFile.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        chooseFile.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { chooseFile.setForeground(new Color(0x1A1A18)); }
            @Override public void mouseExited (java.awt.event.MouseEvent e) { chooseFile.setForeground(new Color(0x888780)); }
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
                if (fc.showOpenDialog(dlg) != javax.swing.JFileChooser.APPROVE_OPTION) return;
                java.io.File f = fc.getSelectedFile();
                if (f.length() > 5L * 1024 * 1024) {
                    JOptionPane.showMessageDialog(dlg, I18n.t("attachment.max_size")); return;
                }
                try {
                    byte[] data = java.nio.file.Files.readAllBytes(f.toPath());
                    Attachment a = new Attachment();
                    a.setFilename(f.getName());
                    String mime = java.net.URLConnection.guessContentTypeFromName(f.getName());
                    a.setMimeType(mime != null ? mime : "application/octet-stream");
                    a.setFileSize((int) f.length());
                    a.setFileData(data);
                    pendingAttachments.add(a);
                    JLabel chip = new JLabel("📎 " + f.getName() + "  (" + a.getFileSizeDisplay() + ")");
                    chip.setFont(new Font("Arial", Font.PLAIN, 11));
                    chip.setForeground(new Color(0x1A1A18));
                    attachChips.add(chip);
                    attachChips.revalidate(); attachChips.repaint();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(dlg, "Error reading file: " + ex.getMessage());
                }
            }
        });

        JPanel attachPanel = new JPanel(new BorderLayout(0, 4));
        attachPanel.setBackground(new Color(0xFAFAF8));
        attachPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE8E8E5)),
                new EmptyBorder(8, 10, 8, 10)));
        attachPanel.add(attachChips,  BorderLayout.CENTER);
        attachPanel.add(chooseFile,   BorderLayout.SOUTH);

        gc.gridy = 13; form.add(inputLabel(I18n.t("task.attachments")), gc);
        gc.gridy = 14; form.add(attachPanel, gc);

        // Wrap form in a scroll pane so it stays compact even with many fields
        JScrollPane formScroll = new JScrollPane(form);
        formScroll.setBorder(null);
        formScroll.getVerticalScrollBar().setUnitIncrement(12);
        formScroll.getViewport().setBackground(Color.WHITE);
        root.add(formScroll, BorderLayout.CENTER);

        // Buttons
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setBackground(Color.WHITE);
        btns.setBorder(new EmptyBorder(16, 0, 0, 0));
        JButton cancel = new JButton(I18n.t("btn.cancel"));
        cancel.setFocusPainted(false);
        cancel.addActionListener(e -> dlg.dispose());
        JButton ok = new JButton(I18n.t("btn.add"));
        ok.setBackground(new Color(0x1A1A18));
        ok.setForeground(Color.WHITE);
        ok.setBorderPainted(false);
        ok.setFocusPainted(false);
        ok.setFont(new Font("Arial", Font.BOLD, 13));
        ok.addActionListener(e -> {
            String title = titleField.getText().trim();
            if (title.isEmpty()) { titleField.requestFocus(); return; }
            int colIdx = colBox.getSelectedIndex();
            Status status = columns.get(Math.min(colIdx, columns.size() - 1)).getStatus();
            Task t = new Task();
            t.setProjectId(project.getProjectId());
            t.setTitle(title);
            t.setDescription(descField.getText().trim());
            t.setPriority(prioBox.getSelectedIndex() + 1);
            t.setWeight((Integer) weightSpinner.getValue());
            if (!noDueDate.isSelected()) t.setDueDate((java.util.Date) dateModel.getValue());
            t.setStatusId(status.getStatusId());
            dlg.dispose();
            new SwingWorker<Task, Void>() {
                @Override protected Task doInBackground() throws Exception {
                    var r = ServerConnection.getInstance().send("CREATE_TASK", t);
                    return r.isSuccess() ? (Task) r.getData() : null;
                }
                @Override protected void done() {
                    try {
                        Task created = get();
                        if (created != null) {
                            addTaskToColumn(created);
                            ToastManager.success(I18n.t("toast.task_created"));
                            if (!pendingAttachments.isEmpty()) {
                                final int newTaskId = created.getTaskId();
                                new SwingWorker<Void, Void>() {
                                    @Override protected Void doInBackground() throws Exception {
                                        for (Attachment a : pendingAttachments) {
                                            a.setTaskId(newTaskId);
                                            ServerConnection.getInstance().send("ADD_ATTACHMENT", a);
                                        }
                                        return null;
                                    }
                                }.execute();
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }.execute();
        });
        btns.add(cancel);
        btns.add(ok);
        root.add(btns, BorderLayout.SOUTH);

        dlg.setContentPane(root);
        dlg.getRootPane().setDefaultButton(ok);
        titleField.requestFocusInWindow();
        dlg.setVisible(true);
    }

    private JTextField styledInput() {
        JTextField f = new JTextField();
        f.setFont(new Font("Arial", Font.PLAIN, 13));
        f.setBackground(new Color(0xFAFAF8));
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0xE8E8E5)),
            new EmptyBorder(7, 10, 7, 10)));
        f.setPreferredSize(new Dimension(0, 34));
        return f;
    }

    private JLabel inputLabel(String text) {
        JLabel l = new JLabel(text.toUpperCase());
        l.setFont(new Font("Arial", Font.BOLD, 10));
        l.setForeground(new Color(0x888780));
        l.setBorder(new EmptyBorder(0, 0, 3, 0));
        return l;
    }

    // ── column/card management ────────────────────────────────────────────────

    private void buildColumns(List<Status> statuses, List<Task> tasks) {
        columns.clear();
        columnsArea.removeAll();
        // +1 for the "Add Column" button at the end
        columnsArea.setLayout(new GridLayout(1, statuses.size() + 1, 12, 0));

        for (Status status : statuses) {
            Consumer<Void> addCallback = v -> showNewTaskDialog(status.getStatusId());
            KanbanColumn col = new KanbanColumn(status, addCallback,
                    this::onCardDropped, this::onCardClicked,
                    this::onCardMarkedDone, this::load);
            columns.add(col);
            columnsArea.add(col);
        }

        // "Add Column" button at the end
        columnsArea.add(buildAddColumnButton());

        // Distribute tasks to columns
        for (Task task : tasks) {
            for (KanbanColumn col : columns) {
                if (task.getStatusId() != null &&
                        task.getStatusId() == col.getStatus().getStatusId()) {
                    col.addTask(task);
                    break;
                }
            }
        }

        columnsArea.revalidate();
        columnsArea.repaint();
    }

    private void addTaskToColumn(Task task) {
        for (KanbanColumn col : columns) {
            if (task.getStatusId() != null &&
                    task.getStatusId() == col.getStatus().getStatusId()) {
                col.addTask(task);
                return;
            }
        }
        // fallback: add to first column
        if (!columns.isEmpty()) columns.get(0).addTask(task);
    }

    private void onCardDropped(int taskId, int newStatusId) {
        Project project = AppContext.getInstance().getCurrentProject();
        if (project == null) return;

        // Move card in UI immediately (optimistic)
        Task movedTask = null;
        for (KanbanColumn col : columns) {
            // find and remove from old column
            // we don't have the task reference easily, so we'll reload after server confirms
        }

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                var r = ServerConnection.getInstance().send("UPDATE_TASK_STATUS",
                        new int[]{taskId, newStatusId, project.getProjectId()});
                return r.isSuccess();
            }
            @Override
            protected void done() {
                try {
                    if (get()) load(); // full reload to show updated state
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void onCardMarkedDone(Task task) {
        Project project = AppContext.getInstance().getCurrentProject();
        if (project == null) return;
        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() throws Exception {
                var r = ServerConnection.getInstance().send("MARK_TASK_DONE",
                        new int[]{task.getTaskId(), project.getProjectId()});
                return r.isSuccess();
            }
            @Override protected void done() {
                try {
                    if (get()) { ToastManager.success(I18n.t("toast.task_done")); load(); }
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private JPanel buildAddColumnButton() {
        JPanel addCol = new JPanel(new GridBagLayout());
        addCol.setOpaque(false);
        addCol.setMinimumSize(new Dimension(160, 0));
        addCol.setPreferredSize(new Dimension(180, 0));

        JLabel lbl = new JLabel(I18n.t("kanban.add_column"));
        lbl.setFont(new Font("Arial", Font.PLAIN, 13));
        lbl.setForeground(new Color(0x888780));
        lbl.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createDashedBorder(new Color(0xE8E8E5), 6, 4),
                new EmptyBorder(12, 18, 12, 18)));
        lbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lbl.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { showAddColumnDialog(); }
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { lbl.setForeground(new Color(0x1A1A18)); }
            @Override public void mouseExited(java.awt.event.MouseEvent e)  { lbl.setForeground(new Color(0x888780)); }
        });
        addCol.add(lbl);
        return addCol;
    }

    private void showAddColumnDialog() {
        Project project = AppContext.getInstance().getCurrentProject();
        if (project == null) return;
        String name = JOptionPane.showInputDialog(this, I18n.t("kanban.add_column"),
                I18n.t("kanban.add_column"), JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.isBlank()) return;
        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() throws Exception {
                var r = ServerConnection.getInstance().send("CREATE_STATUS",
                        new Object[]{project.getProjectId(), name.trim(), "#9CA3AF"});
                return r.isSuccess();
            }
            @Override protected void done() {
                try { if (get()) { ToastManager.success(I18n.t("toast.column_added")); load(); } }
                catch (Exception ignored) {}
            }
        }.execute();
    }

    private void onCardClicked(Task task) {
        Window parent = SwingUtilities.getWindowAncestor(this);
        Frame frame = parent instanceof Frame ? (Frame) parent : null;
        TaskDetailDialog dlg = new TaskDetailDialog(frame, task);
        dlg.setVisible(true);
        // After closing dialog, reload in case data changed
        load();
    }

    // ── real-time event subscriptions ────────────────────────────────────────

    private void subscribeToEvents() {
        EventBus.subscribe(EventBus.TASK_CREATED, data -> {
            if (!loaded) return;
            if (data instanceof Task t &&
                    AppContext.getInstance().getCurrentProject() != null &&
                    t.getProjectId() == AppContext.getInstance().getCurrentProject().getProjectId()) {
                addTaskToColumn(t);
            }
        });

        EventBus.subscribe(EventBus.TASK_STATUS_CHANGED, data -> {
            if (!loaded) return;
            // Simplest approach: full reload
            load();
        });

        EventBus.subscribe(EventBus.TASK_UPDATED, data -> {
            if (!loaded) return;
            load();
        });

        EventBus.subscribe(EventBus.TASK_DELETED, data -> {
            if (!loaded) return;
            load();
        });
    }

    private void showError(String msg) {
        JLabel lbl = new JLabel(msg, SwingConstants.CENTER);
        lbl.setForeground(Color.RED);
        columnsArea.removeAll();
        columnsArea.add(lbl);
        columnsArea.revalidate();
    }

    private record LoadResult(List<Status> statuses, List<Task> tasks) {}
}
