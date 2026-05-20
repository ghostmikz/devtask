package mongolup.client.ui.panels.kanban;

import mongolup.client.AppContext;
import mongolup.client.ServerConnection;
import mongolup.client.i18n.I18n;
import mongolup.client.ui.components.ToastManager;
import mongolup.server.model.Attachment;
import mongolup.server.model.Comment;
import mongolup.server.model.Label;
import mongolup.server.model.Task;
import mongolup.server.model.TaskDetail;
import mongolup.server.model.User;
import mongolup.client.ui.components.AvatarLabel;
import mongolup.client.ui.components.TagLabel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.List;

@SuppressWarnings("unchecked")
public class TaskDetailDialog extends JDialog {

    private static final Color BG       = new Color(0xF5F5F3);
    private static final Color CARD     = Color.WHITE;
    private static final Color BORDER   = new Color(0xE8E8E5);
    private static final Color TEXT_PRI = new Color(0x1A1A18);
    private static final Color TEXT_SEC = new Color(0x888780);
    private static final Color ACCENT   = new Color(0x1A1A18);
    private static final Color SUCCESS  = new Color(0x0F6E56);
    private static final Color INPUT_BG = new Color(0xFAFAF8);

    private final Task task;
    private TaskDetail detail;

    // edit fields
    private JTextField        titleField;
    private JTextArea         descArea;
    private JComboBox<String> priorityBox;
    private JSpinner          dueDateSpinner;
    private JCheckBox         noDueDateBox;
    private JTextField        weightField;

    // sections
    private JPanel assigneesPanel;
    private JPanel labelsPanel;
    private JPanel attachmentsPanel;
    private JPanel commentsPanel;
    private JTextField commentInput;

    public TaskDetailDialog(Frame parent, Task task) {
        super(parent, I18n.t("task.title"), true);
        this.task = task;
        setSize(620, 720);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());
        getContentPane().setBackground(BG);
        buildUI();
        loadDetail();
    }

    private void buildUI() {
        // Scrollable main content
        JPanel content = new JPanel();
        content.setBackground(BG);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(20, 20, 20, 20));

        // ── Title ────────────────────────────────────────────────────────────
        content.add(sectionCard(() -> {
            JPanel p = new JPanel(new BorderLayout(0, 8));
            p.setBackground(CARD);
            JLabel lbl = fieldLabel(I18n.t("task.title"));
            titleField = new JTextField(task.getTitle());
            styleInput(titleField);
            p.add(lbl, BorderLayout.NORTH);
            p.add(titleField, BorderLayout.CENTER);
            return p;
        }));
        content.add(Box.createVerticalStrut(10));

        // ── Description ──────────────────────────────────────────────────────
        content.add(sectionCard(() -> {
            JPanel p = new JPanel(new BorderLayout(0, 8));
            p.setBackground(CARD);
            p.add(fieldLabel(I18n.t("task.description")), BorderLayout.NORTH);
            descArea = new JTextArea(task.getDescription() != null ? task.getDescription() : "", 4, 0);
            descArea.setFont(new Font("Arial", Font.PLAIN, 13));
            descArea.setBackground(INPUT_BG);
            descArea.setBorder(new EmptyBorder(8, 10, 8, 10));
            descArea.setLineWrap(true);
            descArea.setWrapStyleWord(true);
            JScrollPane descScroll = new JScrollPane(descArea);
            descScroll.setBorder(BorderFactory.createLineBorder(BORDER));
            descScroll.getViewport().setBackground(INPUT_BG);
            descScroll.setPreferredSize(new Dimension(0, 100));
            p.add(descScroll, BorderLayout.CENTER);
            return p;
        }));
        content.add(Box.createVerticalStrut(10));

        // ── Meta row ──────────────────────────────────────────────────────────
        content.add(sectionCard(() -> {
            JPanel p = new JPanel(new GridLayout(1, 3, 12, 0));
            p.setBackground(CARD);

            priorityBox = new JComboBox<>(new String[]{"1 - " + I18n.t("priority.low"),
                    "2 - " + I18n.t("priority.medium"),
                    "3 - " + I18n.t("priority.high"),
                    "4 - " + I18n.t("priority.urgent")});
            priorityBox.setSelectedIndex(Math.max(0, task.getPriority() - 1));
            priorityBox.setBackground(INPUT_BG);

            // Date+time spinner — defaults to today when task has no due date
            SpinnerDateModel dateModel = new SpinnerDateModel();
            dateModel.setValue(task.getDueDate() != null ? task.getDueDate() : new java.util.Date());
            dueDateSpinner = new JSpinner(dateModel);
            JSpinner.DateEditor dateEd = new JSpinner.DateEditor(dueDateSpinner, "yyyy-MM-dd HH:mm");
            dueDateSpinner.setEditor(dateEd);
            dateEd.getTextField().setBackground(INPUT_BG);
            dateEd.getTextField().setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER),
                    new EmptyBorder(5, 8, 5, 8)));
            dueDateSpinner.setBorder(null);

            // "No due date" checkbox stacked below spinner — wrapped so grid heights stay sane
            noDueDateBox = new JCheckBox(I18n.t("task.no_due_date"), task.getDueDate() == null);
            noDueDateBox.setBackground(CARD);
            noDueDateBox.setFont(new Font("Arial", Font.PLAIN, 10));
            noDueDateBox.addActionListener(e -> dueDateSpinner.setEnabled(!noDueDateBox.isSelected()));
            dueDateSpinner.setEnabled(!noDueDateBox.isSelected());

            JPanel dateCell = new JPanel(new BorderLayout(0, 2));
            dateCell.setBackground(CARD);
            dateCell.add(dueDateSpinner, BorderLayout.CENTER);
            dateCell.add(noDueDateBox,   BorderLayout.SOUTH);

            weightField = new JTextField(String.valueOf(task.getWeight()));
            styleInput(weightField);

            p.add(fieldGroupSmall(I18n.t("task.priority"), priorityBox));
            p.add(fieldGroupSmall(I18n.t("task.due_date"), dateCell));
            p.add(fieldGroupSmall(I18n.t("task.points"),   weightField));
            return p;
        }));
        content.add(Box.createVerticalStrut(10));

        // ── Assignees ────────────────────────────────────────────────────────
        content.add(sectionCard(() -> {
            JPanel p = new JPanel(new BorderLayout(0, 8));
            p.setBackground(CARD);

            // header row: label + "+ Assign" button side by side
            JPanel hdr = new JPanel(new BorderLayout());
            hdr.setBackground(CARD);
            hdr.add(fieldLabel(I18n.t("task.assignees")), BorderLayout.WEST);
            JButton addBtn = new JButton("+ " + I18n.t("btn.assign"));
            addBtn.setFont(new Font("Arial", Font.PLAIN, 11));
            addBtn.setForeground(SUCCESS);
            addBtn.setBackground(CARD);
            addBtn.setBorderPainted(false);
            addBtn.setContentAreaFilled(false);
            addBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addBtn.addActionListener(e -> showAssigneeDialog());
            hdr.add(addBtn, BorderLayout.EAST);
            p.add(hdr, BorderLayout.NORTH);

            assigneesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
            assigneesPanel.setBackground(CARD);
            // placeholder shown until server responds
            JLabel placeholder = new JLabel(I18n.t("task.no_assignees"));
            placeholder.setFont(new Font("Arial", Font.ITALIC, 12));
            placeholder.setForeground(TEXT_SEC);
            assigneesPanel.add(placeholder);
            p.add(assigneesPanel, BorderLayout.CENTER);
            return p;
        }));
        content.add(Box.createVerticalStrut(10));

        // ── Labels ───────────────────────────────────────────────────────────
        content.add(sectionCard(() -> {
            JPanel p = new JPanel(new BorderLayout(0, 8));
            p.setBackground(CARD);

            JPanel hdr = new JPanel(new BorderLayout());
            hdr.setBackground(CARD);
            hdr.add(fieldLabel(I18n.t("task.labels")), BorderLayout.WEST);
            JButton addLblBtn = new JButton("+ " + I18n.t("btn.add_label"));
            addLblBtn.setFont(new Font("Arial", Font.PLAIN, 11));
            addLblBtn.setForeground(SUCCESS);
            addLblBtn.setBackground(CARD);
            addLblBtn.setBorderPainted(false);
            addLblBtn.setContentAreaFilled(false);
            addLblBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addLblBtn.addActionListener(e -> showLabelDialog());
            hdr.add(addLblBtn, BorderLayout.EAST);
            p.add(hdr, BorderLayout.NORTH);

            labelsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 4));
            labelsPanel.setBackground(CARD);
            JLabel ph = new JLabel(I18n.t("task.no_labels"));
            ph.setFont(new Font("Arial", Font.ITALIC, 12));
            ph.setForeground(TEXT_SEC);
            labelsPanel.add(ph);
            p.add(labelsPanel, BorderLayout.CENTER);
            return p;
        }));
        content.add(Box.createVerticalStrut(10));

        // ── Attachments ──────────────────────────────────────────────────────
        content.add(sectionCard(() -> {
            JPanel p = new JPanel(new BorderLayout(0, 8));
            p.setBackground(CARD);
            p.add(fieldLabel(I18n.t("task.attachments")), BorderLayout.NORTH);
            attachmentsPanel = new JPanel();
            attachmentsPanel.setBackground(CARD);
            attachmentsPanel.setLayout(new BoxLayout(attachmentsPanel, BoxLayout.Y_AXIS));
            p.add(attachmentsPanel, BorderLayout.CENTER);
            return p;
        }));
        content.add(Box.createVerticalStrut(10));

        // ── Comments ─────────────────────────────────────────────────────────
        content.add(sectionCard(() -> {
            JPanel p = new JPanel(new BorderLayout(0, 8));
            p.setBackground(CARD);
            p.add(fieldLabel(I18n.t("task.comments")), BorderLayout.NORTH);
            commentsPanel = new JPanel();
            commentsPanel.setBackground(CARD);
            commentsPanel.setLayout(new BoxLayout(commentsPanel, BoxLayout.Y_AXIS));
            p.add(commentsPanel, BorderLayout.CENTER);

            JPanel inputRow = new JPanel(new BorderLayout(8, 0));
            inputRow.setBackground(CARD);
            inputRow.setBorder(new EmptyBorder(8, 0, 0, 0));
            commentInput = new JTextField();
            commentInput.setBackground(INPUT_BG);
            commentInput.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER),
                    new EmptyBorder(6, 10, 6, 10)));
            commentInput.setFont(new Font("Arial", Font.PLAIN, 12));
            JButton send = new JButton(I18n.t("task.send"));
            send.setBackground(ACCENT);
            send.setForeground(Color.WHITE);
            send.setBorderPainted(false);
            send.setFocusPainted(false);
            send.addActionListener(e -> postComment());
            commentInput.addActionListener(e -> postComment());
            inputRow.add(commentInput, BorderLayout.CENTER);
            inputRow.add(send, BorderLayout.EAST);
            p.add(inputRow, BorderLayout.SOUTH);
            return p;
        }));
        content.add(Box.createVerticalStrut(20));

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        // ── Button bar ────────────────────────────────────────────────────────
        JPanel btnBar = new JPanel(new BorderLayout());
        btnBar.setBackground(BG);
        btnBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER));

        // Delete button on the left
        JButton delete = new JButton(I18n.t("btn.delete"));
        delete.setForeground(new Color(0xA32D2D));
        delete.setBackground(new Color(0xFEF2F2));
        delete.setBorderPainted(false);
        delete.setFocusPainted(false);
        delete.setFont(new Font("Arial", Font.PLAIN, 12));
        delete.setBorder(new EmptyBorder(8, 16, 8, 16));
        delete.addActionListener(e -> deleteTask());

        JPanel leftBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        leftBtns.setBackground(BG);
        leftBtns.add(delete);

        // Cancel / Save on the right
        JPanel rightBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        rightBtns.setBackground(BG);
        JButton cancel = new JButton(I18n.t("btn.cancel"));
        cancel.addActionListener(e -> dispose());
        JButton save = new JButton(I18n.t("btn.save"));
        save.setBackground(ACCENT);
        save.setForeground(Color.WHITE);
        save.setBorderPainted(false);
        save.setFocusPainted(false);
        save.addActionListener(e -> saveTask());
        rightBtns.add(cancel);
        rightBtns.add(save);

        btnBar.add(leftBtns, BorderLayout.WEST);
        btnBar.add(rightBtns, BorderLayout.EAST);
        add(btnBar, BorderLayout.SOUTH);
    }

    // ── data loading ─────────────────────────────────────────────────────────

    private void loadDetail() {
        new SwingWorker<TaskDetail, Void>() {
            @Override protected TaskDetail doInBackground() throws Exception {
                var r = ServerConnection.getInstance().send("GET_TASK_DETAIL", task.getTaskId());
                return r.isSuccess() ? (TaskDetail) r.getData() : null;
            }
            @Override protected void done() {
                try {
                    detail = get();
                    if (detail != null) {
                        populateAssignees(detail.getAssignees());
                        populateLabels(detail.getLabels());
                        populateAttachments(detail.getAttachments());
                        populateComments(detail.getComments());
                        // Force the scroll pane to re-measure all card sizes
                        SwingUtilities.invokeLater(() -> {
                            revalidate();
                            repaint();
                        });
                    }
                } catch (Exception e) {
                    // Surface the error so we can diagnose — show in assignees area
                    if (assigneesPanel != null) {
                        assigneesPanel.removeAll();
                        JLabel err = new JLabel("Load error: " + e.getMessage());
                        err.setForeground(new Color(0xA32D2D));
                        err.setFont(new Font("Arial", Font.PLAIN, 11));
                        assigneesPanel.add(err);
                        assigneesPanel.revalidate();
                    }
                }
            }
        }.execute();
    }

    private void populateAssignees(List<User> assignees) {
        assigneesPanel.removeAll();
        boolean hasAny = assignees != null && !assignees.isEmpty();
        if (hasAny) {
            for (User u : assignees) {
                Color av = avatarColor(u.getUserId());
                AvatarLabel a = new AvatarLabel(u.getInitials(), av, 28);
                a.setToolTipText(u.getFullName() != null ? u.getFullName() : u.getUsername());
                // click avatar to remove
                a.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                final int uid = u.getUserId();
                a.addMouseListener(new MouseAdapter() {
                    @Override public void mouseClicked(MouseEvent e) {
                        int r = JOptionPane.showConfirmDialog(TaskDetailDialog.this,
                                I18n.t("task.remove_assignee") + " " +
                                (u.getFullName() != null ? u.getFullName() : u.getUsername()) + "?",
                                "", JOptionPane.YES_NO_OPTION);
                        if (r == JOptionPane.YES_OPTION) {
                            new SwingWorker<Void, Void>() {
                                @Override protected Void doInBackground() throws Exception {
                                    ServerConnection.getInstance().send("REMOVE_ASSIGNEE",
                                            new int[]{task.getTaskId(), uid});
                                    return null;
                                }
                                @Override protected void done() { loadDetail(); }
                            }.execute();
                        }
                    }
                });
                assigneesPanel.add(a);
            }
        } else {
            JLabel empty = new JLabel(I18n.t("task.no_assignees"));
            empty.setFont(new Font("Arial", Font.ITALIC, 12));
            empty.setForeground(TEXT_SEC);
            assigneesPanel.add(empty);
        }
        assigneesPanel.revalidate();
        assigneesPanel.repaint();
    }

    private void populateAttachments(List<Attachment> attachments) {
        attachmentsPanel.removeAll();
        if (attachments != null) {
            for (Attachment a : attachments) {
                JPanel chip = new JPanel(new BorderLayout(6, 0));
                chip.setBackground(new Color(0xF0F0EE));
                chip.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER),
                        new EmptyBorder(4, 8, 4, 8)));
                chip.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
                chip.setAlignmentX(LEFT_ALIGNMENT);
                JLabel nameLbl = new JLabel("📎 " + a.getFilename()
                        + "  (" + a.getFileSizeDisplay() + ")");
                nameLbl.setFont(new Font("Arial", Font.PLAIN, 11));
                nameLbl.setForeground(TEXT_PRI);
                nameLbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                nameLbl.addMouseListener(new MouseAdapter() {
                    @Override public void mouseClicked(MouseEvent e) { downloadAttachment(a); }
                });
                JLabel del = new JLabel(" ✕");
                del.setFont(new Font("Arial", Font.PLAIN, 10));
                del.setForeground(TEXT_SEC);
                del.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                del.addMouseListener(new MouseAdapter() {
                    @Override public void mouseClicked(MouseEvent e) {
                        new SwingWorker<Boolean, Void>() {
                            @Override protected Boolean doInBackground() throws Exception {
                                var r = ServerConnection.getInstance()
                                        .send("DELETE_ATTACHMENT", a.getAttachmentId());
                                return r.isSuccess();
                            }
                            @Override protected void done() {
                                try { if (get()) loadDetail(); } catch (Exception ignored) {}
                            }
                        }.execute();
                    }
                });
                chip.add(nameLbl, BorderLayout.CENTER);
                chip.add(del,     BorderLayout.EAST);
                attachmentsPanel.add(chip);
                attachmentsPanel.add(Box.createVerticalStrut(4));
            }
        }
        // Add-file link
        JLabel addFile = new JLabel(I18n.t("task.add_attachment"));
        addFile.setFont(new Font("Arial", Font.PLAIN, 12));
        addFile.setForeground(TEXT_SEC);
        addFile.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addFile.setAlignmentX(LEFT_ALIGNMENT);
        addFile.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { addFile.setForeground(ACCENT); }
            @Override public void mouseExited (MouseEvent e) { addFile.setForeground(TEXT_SEC); }
            @Override public void mouseClicked(MouseEvent e) { uploadAttachment(); }
        });
        attachmentsPanel.add(addFile);
        attachmentsPanel.revalidate();
        attachmentsPanel.repaint();
    }

    private void uploadAttachment() {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        if (f.length() > 5L * 1024 * 1024) {
            JOptionPane.showMessageDialog(this, I18n.t("attachment.max_size")); return;
        }
        try {
            byte[] data = Files.readAllBytes(f.toPath());
            Attachment a = new Attachment();
            a.setTaskId(task.getTaskId());
            a.setFilename(f.getName());
            String mime = java.net.URLConnection.guessContentTypeFromName(f.getName());
            a.setMimeType(mime != null ? mime : "application/octet-stream");
            a.setFileSize((int) f.length());
            a.setFileData(data);
            new SwingWorker<Attachment, Void>() {
                @Override protected Attachment doInBackground() throws Exception {
                    var r = ServerConnection.getInstance().send("ADD_ATTACHMENT", a);
                    return r.isSuccess() ? (Attachment) r.getData() : null;
                }
                @Override protected void done() {
                    try {
                        if (get() != null) {
                            ToastManager.success(I18n.t("toast.attachment_added"));
                            loadDetail();
                        }
                    } catch (Exception ignored) {}
                }
            }.execute();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error reading file: " + ex.getMessage());
        }
    }

    private void downloadAttachment(Attachment meta) {
        new SwingWorker<Attachment, Void>() {
            @Override protected Attachment doInBackground() throws Exception {
                var r = ServerConnection.getInstance()
                        .send("GET_ATTACHMENT_FILE", meta.getAttachmentId());
                return r.isSuccess() ? (Attachment) r.getData() : null;
            }
            @Override protected void done() {
                try {
                    Attachment full = get();
                    if (full == null || full.getFileData() == null) return;
                    File tmp = File.createTempFile("devtask_", "_" + full.getFilename());
                    tmp.deleteOnExit();
                    try (FileOutputStream fos = new FileOutputStream(tmp)) {
                        fos.write(full.getFileData());
                    }
                    if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(tmp);
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void populateLabels(List<Label> labels) {
        labelsPanel.removeAll();
        boolean hasAny = labels != null && !labels.isEmpty();
        if (hasAny) {
            for (Label l : labels) {
                Color bg = parseHex(l.getColor(), new Color(0xE8E8E5));
                Color fg = darken(bg);

                // chip = tag + "✕" remove button
                JPanel chip = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
                chip.setBackground(bg);
                chip.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 4));
                JLabel tag = new JLabel(l.getName());
                tag.setFont(new Font("Arial", Font.BOLD, 11));
                tag.setForeground(fg);
                JLabel rm = new JLabel("✕");
                rm.setFont(new Font("Arial", Font.PLAIN, 10));
                rm.setForeground(fg);
                rm.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                rm.addMouseListener(new MouseAdapter() {
                    @Override public void mouseClicked(MouseEvent e) {
                        new SwingWorker<Void, Void>() {
                            @Override protected Void doInBackground() throws Exception {
                                ServerConnection.getInstance().send("REMOVE_LABEL_FROM_TASK",
                                        new int[]{task.getTaskId(), l.getLabelId()});
                                return null;
                            }
                            @Override protected void done() { loadDetail(); }
                        }.execute();
                    }
                });
                chip.add(tag);
                chip.add(rm);
                labelsPanel.add(chip);
            }
        } else {
            JLabel empty = new JLabel(I18n.t("task.no_labels"));
            empty.setFont(new Font("Arial", Font.ITALIC, 12));
            empty.setForeground(TEXT_SEC);
            labelsPanel.add(empty);
        }
        labelsPanel.revalidate();
        labelsPanel.repaint();
    }

    private void showLabelDialog() {
        int projectId = task.getProjectId();
        new SwingWorker<List<Label>, Void>() {
            @Override protected List<Label> doInBackground() throws Exception {
                var r = ServerConnection.getInstance().send("GET_LABELS", projectId);
                return r.isSuccess() ? (List<Label>) r.getData() : List.of();
            }
            @Override protected void done() {
                try {
                    List<Label> all = get();
                    // filter out already-applied labels
                    List<Label> applied = (detail != null && detail.getLabels() != null)
                            ? detail.getLabels() : List.of();
                    List<Label> available = all.stream()
                            .filter(l -> applied.stream().noneMatch(a -> a.getLabelId() == l.getLabelId()))
                            .toList();
                    if (available.isEmpty()) {
                        // no project labels exist yet — offer to create one
                        showCreateLabelDialog();
                    } else {
                        // let user pick from existing, or create a new one
                        String[] names = available.stream().map(Label::getName).toArray(String[]::new);
                        String[] opts  = new String[names.length + 1];
                        System.arraycopy(names, 0, opts, 0, names.length);
                        opts[names.length] = "+ " + I18n.t("task.create_label");

                        String chosen = (String) JOptionPane.showInputDialog(
                                TaskDetailDialog.this, I18n.t("task.labels"),
                                I18n.t("btn.add_label"),
                                JOptionPane.PLAIN_MESSAGE, null, opts, opts[0]);
                        if (chosen == null) return;
                        if (chosen.startsWith("+ ")) { showCreateLabelDialog(); return; }

                        for (int i = 0; i < names.length; i++) {
                            if (names[i].equals(chosen)) {
                                final int lid = available.get(i).getLabelId();
                                new SwingWorker<Void, Void>() {
                                    @Override protected Void doInBackground() throws Exception {
                                        ServerConnection.getInstance().send("ADD_LABEL_TO_TASK",
                                                new int[]{task.getTaskId(), lid});
                                        return null;
                                    }
                                    @Override protected void done() { loadDetail(); }
                                }.execute();
                                break;
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void showCreateLabelDialog() {
        // preset palette
        String[] colorHex   = {"#3B82F6","#10B981","#F59E0B","#EF4444","#8B5CF6","#EC4899","#6B7280"};
        String[] colorNames = {"Blue","Green","Yellow","Red","Purple","Pink","Gray"};

        JTextField nameField = new JTextField(14);
        JComboBox<String> colorBox = new JComboBox<>(colorNames);
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel(I18n.t("task.label_name") + ":"));
        form.add(nameField);
        form.add(new JLabel(I18n.t("task.label_color") + ":"));
        form.add(colorBox);

        int res = JOptionPane.showConfirmDialog(this, form,
                I18n.t("task.create_label"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return;
        String name = nameField.getText().trim();
        if (name.isEmpty()) return;
        String color = colorHex[colorBox.getSelectedIndex()];

        Label newLabel = new Label();
        newLabel.setProjectId(task.getProjectId());
        newLabel.setName(name);
        newLabel.setColor(color);

        new SwingWorker<Label, Void>() {
            @Override protected Label doInBackground() throws Exception {
                var r = ServerConnection.getInstance().send("CREATE_LABEL", newLabel);
                return r.isSuccess() ? (Label) r.getData() : null;
            }
            @Override protected void done() {
                try {
                    Label created = get();
                    if (created == null) return;
                    new SwingWorker<Void, Void>() {
                        @Override protected Void doInBackground() throws Exception {
                            ServerConnection.getInstance().send("ADD_LABEL_TO_TASK",
                                    new int[]{task.getTaskId(), created.getLabelId()});
                            return null;
                        }
                        @Override protected void done() { loadDetail(); }
                    }.execute();
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void populateComments(List<Comment> comments) {
        commentsPanel.removeAll();
        if (comments != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH:mm");
            for (Comment cm : comments) {
                JPanel row = new JPanel(new BorderLayout(8, 0));
                row.setBackground(CARD);
                row.setBorder(new EmptyBorder(6, 0, 6, 0));
                Color av = avatarColor(cm.getUserId());
                AvatarLabel avatar = new AvatarLabel(
                        cm.getAuthorInitials() != null ? cm.getAuthorInitials() : "?", av, 26);
                JPanel textBlock = new JPanel();
                textBlock.setBackground(CARD);
                textBlock.setLayout(new BoxLayout(textBlock, BoxLayout.Y_AXIS));
                JLabel author = new JLabel(cm.getAuthorName() != null ? cm.getAuthorName() : "");
                author.setFont(new Font("Arial", Font.BOLD, 11));
                author.setForeground(TEXT_PRI);
                JLabel body = new JLabel("<html>" + escapeHtml(cm.getContent()) + "</html>");
                body.setFont(new Font("Arial", Font.PLAIN, 12));
                body.setForeground(TEXT_SEC);
                JLabel time = new JLabel(cm.getCreatedAt() != null ? sdf.format(cm.getCreatedAt()) : "");
                time.setFont(new Font("Arial", Font.PLAIN, 10));
                time.setForeground(TEXT_SEC);
                textBlock.add(author);
                textBlock.add(body);
                textBlock.add(time);
                row.add(avatar, BorderLayout.WEST);
                row.add(textBlock, BorderLayout.CENTER);
                commentsPanel.add(row);
            }
        }
        commentsPanel.revalidate();
        commentsPanel.repaint();
    }

    // ── actions ───────────────────────────────────────────────────────────────

    private void deleteTask() {
        int choice = JOptionPane.showConfirmDialog(this,
                "<html>Delete <b>" + escapeHtml(task.getTitle()) + "</b>?<br>" +
                "This cannot be undone.</html>",
                I18n.t("btn.delete"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) return;

        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() throws Exception {
                var r = ServerConnection.getInstance().send("DELETE_TASK",
                        new int[]{task.getTaskId(), task.getProjectId()});
                return r.isSuccess();
            }
            @Override protected void done() {
                try {
                    if (get()) dispose();
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void saveTask() {
        task.setTitle(titleField.getText().trim());
        task.setDescription(descArea.getText().trim());
        task.setPriority(priorityBox.getSelectedIndex() + 1);
        try { task.setWeight(Integer.parseInt(weightField.getText().trim())); }
        catch (NumberFormatException ignored) {}
        if (noDueDateBox != null && noDueDateBox.isSelected()) {
            task.setDueDate(null);
        } else if (dueDateSpinner != null) {
            task.setDueDate((java.util.Date) ((SpinnerDateModel) dueDateSpinner.getModel()).getValue());
        }

        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                ServerConnection.getInstance().send("UPDATE_TASK", task);
                return null;
            }
            @Override protected void done() {
                ToastManager.success(I18n.t("toast.task_saved"));
                dispose();
            }
        }.execute();
    }

    private void postComment() {
        String text = commentInput.getText().trim();
        if (text.isEmpty()) return;
        commentInput.setText("");
        Comment cm = new Comment();
        cm.setTaskId(task.getTaskId());
        cm.setContent(text);
        new SwingWorker<Comment, Void>() {
            @Override protected Comment doInBackground() throws Exception {
                var r = ServerConnection.getInstance().send("ADD_COMMENT", cm);
                return r.isSuccess() ? (Comment) r.getData() : null;
            }
            @Override protected void done() {
                try {
                    Comment saved = get();
                    if (saved != null) populateComments(
                            detail != null ? appendComment(detail.getComments(), saved)
                                          : List.of(saved));
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void showAssigneeDialog() {
        new SwingWorker<List<User>, Void>() {
            @Override protected List<User> doInBackground() throws Exception {
                // GET_ALL_USERS — shows every active user, not just current project members
                var r = ServerConnection.getInstance().send("GET_ALL_USERS", null);
                return r.isSuccess() ? (List<User>) r.getData() : List.of();
            }
            @Override protected void done() {
                try {
                    List<User> users = get();
                    if (users.isEmpty()) return;
                    // Build display-friendly labels: "Full Name (username)"
                    String[] labels = users.stream()
                            .map(u -> (u.getFullName() != null && !u.getFullName().isBlank()
                                    ? u.getFullName() : u.getUsername())
                                    + " (@" + u.getUsername() + ")")
                            .toArray(String[]::new);
                    String chosen = (String) JOptionPane.showInputDialog(
                            TaskDetailDialog.this,
                            I18n.t("task.assignees"),
                            I18n.t("btn.add"),
                            JOptionPane.PLAIN_MESSAGE, null,
                            labels, labels[0]);
                    if (chosen == null) return;
                    // Map chosen label back to User
                    for (int i = 0; i < labels.length; i++) {
                        if (labels[i].equals(chosen)) {
                            ServerConnection.getInstance().send("ASSIGN_USER",
                                    new int[]{task.getTaskId(), users.get(i).getUserId()});
                            loadDetail();
                            break;
                        }
                    }
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private List<Comment> appendComment(List<Comment> existing, Comment cm) {
        java.util.ArrayList<Comment> list = new java.util.ArrayList<>(
                existing != null ? existing : List.of());
        list.add(cm);
        return list;
    }

    private JPanel sectionCard(java.util.function.Supplier<JPanel> content) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(CARD);
        wrapper.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(14, 14, 14, 14)));
        wrapper.setAlignmentX(LEFT_ALIGNMENT);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 500));
        wrapper.add(content.get());
        return wrapper;
    }

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text.toUpperCase());
        l.setFont(new Font("Arial", Font.BOLD, 10));
        l.setForeground(TEXT_SEC);
        return l;
    }

    private void styleInput(JTextField tf) {
        tf.setBackground(INPUT_BG);
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(7, 10, 7, 10)));
        tf.setFont(new Font("Arial", Font.PLAIN, 13));
    }

    private JPanel fieldGroupSmall(String label, JComponent input) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setBackground(CARD);
        p.add(fieldLabel(label), BorderLayout.NORTH);
        p.add(input, BorderLayout.CENTER);
        return p;
    }

    private Color avatarColor(int id) {
        Color[] pal = {new Color(0x0F6E56), new Color(0x7C3AED),
                       new Color(0x2563EB), new Color(0xD97706)};
        return pal[Math.abs(id) % pal.length];
    }

    private Color parseHex(String hex, Color fallback) {
        try { return hex != null ? Color.decode(hex) : fallback; }
        catch (NumberFormatException e) { return fallback; }
    }

    private Color darken(Color c) {
        return new Color(Math.max(0, c.getRed() - 60),
                         Math.max(0, c.getGreen() - 60),
                         Math.max(0, c.getBlue() - 60));
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
