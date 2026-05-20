package mongolup.client.ui.panels;

import mongolup.client.AppContext;
import mongolup.client.ServerConnection;
import mongolup.client.i18n.I18n;
import mongolup.server.model.Project;
import mongolup.server.model.User;
import mongolup.client.ui.components.AvatarLabel;
import mongolup.client.ui.components.RoundedPanel;
import mongolup.client.ui.components.ToastManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.Dialog;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.function.Consumer;

@SuppressWarnings("unchecked")
public class ProjectsPanel extends JPanel {

    private static final Color BG      = new Color(0xF5F5F3);
    private static final Color CARD    = Color.WHITE;
    private static final Color BORDER  = new Color(0xE8E8E5);
    private static final Color TEXT    = new Color(0x1A1A18);
    private static final Color SEC     = new Color(0x888780);
    private static final Color SUCCESS = new Color(0x0F6E56);
    private static final Color SUC_BG  = new Color(0xE1F5EE);

    private JPanel gridPanel;
    private final Consumer<String> showPage;
    private Runnable onProjectsChanged; // callback to refresh sidebar

    public ProjectsPanel(Consumer<String> showPage) {
        this.showPage = showPage;
        setBackground(BG);
        setLayout(new BorderLayout());
        buildUI();
        I18n.onLocaleChange(this::load);
    }

    public void setOnProjectsChanged(Runnable r) { this.onProjectsChanged = r; }

    private void buildUI() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 18));
        wrapper.setBackground(BG);
        wrapper.setBorder(new EmptyBorder(18, 18, 18, 18));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG);
        JLabel title = new JLabel(I18n.t("projects.title"));
        title.setFont(new Font("Arial", Font.BOLD, 20));
        title.setForeground(TEXT);
        header.add(title, BorderLayout.WEST);
        wrapper.add(header, BorderLayout.NORTH);

        // Grid — wrap in top-aligned panel so cards don't stretch vertically
        gridPanel = new JPanel(new GridLayout(0, 3, 14, 14));
        gridPanel.setBackground(BG);

        JPanel topAligned = new JPanel(new BorderLayout());
        topAligned.setBackground(BG);
        topAligned.add(gridPanel, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(topAligned);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        wrapper.add(scroll, BorderLayout.CENTER);

        add(wrapper, BorderLayout.CENTER);
    }

    public void load() {
        new SwingWorker<List<Project>, Void>() {
            @Override protected List<Project> doInBackground() throws Exception {
                var r = ServerConnection.getInstance().send("GET_PROJECTS", null);
                return r.isSuccess() ? (List<Project>) r.getData() : List.of();
            }
            @Override protected void done() {
                try { render(get()); }
                catch (Exception ignored) {}
            }
        }.execute();
    }

    public void showNewProjectDialog() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dlg = new JDialog(owner, I18n.t("projects.new"), Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setSize(420, 310);
        dlg.setLocationRelativeTo(owner);
        dlg.setResizable(false);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(Color.WHITE);
        root.setBorder(new EmptyBorder(24, 28, 20, 28));

        // Title
        JLabel title = new JLabel(I18n.t("projects.new"));
        title.setFont(new Font("Arial", Font.BOLD, 16));
        title.setForeground(TEXT);
        title.setBorder(new EmptyBorder(0, 0, 18, 0));
        root.add(title, BorderLayout.NORTH);

        // Form
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(0, 0, 10, 0);
        gc.gridx = 0; gc.weightx = 1;

        JTextField nameField = styledField();
        JTextField descField = styledField();
        String[] colors = {"#0F6E56", "#2563EB", "#7C3AED", "#D97706", "#EF4444"};
        JComboBox<String> colorBox = new JComboBox<>(colors);
        colorBox.setBackground(new Color(0xFAFAF8));
        colorBox.setFont(new Font("Arial", Font.PLAIN, 13));

        gc.gridy = 0; form.add(fieldLabel(I18n.t("projects.new.name")), gc);
        gc.gridy = 1; form.add(nameField, gc);
        gc.gridy = 2; form.add(fieldLabel(I18n.t("projects.new.desc")), gc);
        gc.gridy = 3; form.add(descField, gc);
        gc.gridy = 4; form.add(fieldLabel(I18n.t("projects.new.color")), gc);
        gc.gridy = 5; form.add(colorBox, gc);
        root.add(form, BorderLayout.CENTER);

        // Buttons
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setBackground(Color.WHITE);
        btns.setBorder(new EmptyBorder(16, 0, 0, 0));
        JButton cancel = new JButton(I18n.t("btn.cancel"));
        cancel.setFocusPainted(false);
        cancel.addActionListener(e -> dlg.dispose());
        JButton ok = new JButton(I18n.t("btn.save"));
        ok.setBackground(new Color(0x1A1A18));
        ok.setForeground(Color.WHITE);
        ok.setBorderPainted(false);
        ok.setFocusPainted(false);
        ok.setFont(new Font("Arial", Font.BOLD, 13));
        ok.addActionListener(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) { nameField.requestFocus(); return; }
            Project p = new Project();
            p.setName(name);
            p.setDescription(descField.getText().trim());
            p.setColor((String) colorBox.getSelectedItem());
            dlg.dispose();
            new SwingWorker<Project, Void>() {
                @Override protected Project doInBackground() throws Exception {
                    var r = ServerConnection.getInstance().send("CREATE_PROJECT", p);
                    return r.isSuccess() ? (Project) r.getData() : null;
                }
                @Override protected void done() {
                    try {
                        if (get() != null) {
                            load();
                            if (onProjectsChanged != null) onProjectsChanged.run();
                        }
                    } catch (Exception ignored) {}
                }
            }.execute();
        });
        btns.add(cancel);
        btns.add(ok);
        root.add(btns, BorderLayout.SOUTH);

        dlg.setContentPane(root);
        nameField.requestFocusInWindow();
        dlg.setVisible(true);
    }

    private JTextField styledField() {
        JTextField f = new JTextField();
        f.setFont(new Font("Arial", Font.PLAIN, 13));
        f.setBackground(new Color(0xFAFAF8));
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0xE8E8E5)),
            new EmptyBorder(7, 10, 7, 10)));
        f.setPreferredSize(new Dimension(0, 34));
        return f;
    }

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text.toUpperCase());
        l.setFont(new Font("Arial", Font.BOLD, 10));
        l.setForeground(new Color(0x888780));
        l.setBorder(new EmptyBorder(0, 0, 3, 0));
        return l;
    }

    private void render(List<Project> projects) {
        gridPanel.removeAll();
        for (Project p : projects) {
            gridPanel.add(buildProjectCard(p));
        }
        // "+ New project" tile — fixed height matches a typical card
        JPanel addCard = new JPanel(new GridBagLayout());
        addCard.setBackground(CARD);
        addCard.setPreferredSize(new Dimension(0, 180));
        addCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createDashedBorder(BORDER, 6, 4),
                new EmptyBorder(20, 20, 20, 20)));
        addCard.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setBackground(CARD);
        JLabel plus = new JLabel("+");
        plus.setFont(new Font("Arial", Font.PLAIN, 32));
        plus.setForeground(SEC);
        plus.setAlignmentX(CENTER_ALIGNMENT);
        JLabel addLbl = new JLabel(I18n.t("projects.new"));
        addLbl.setFont(new Font("Arial", Font.PLAIN, 13));
        addLbl.setForeground(SEC);
        addLbl.setAlignmentX(CENTER_ALIGNMENT);
        inner.add(plus);
        inner.add(Box.createVerticalStrut(6));
        inner.add(addLbl);
        addCard.add(inner);

        addCard.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { showNewProjectDialog(); }
            @Override public void mouseEntered(MouseEvent e) { addCard.setBackground(new Color(0xFAFAF8)); inner.setBackground(new Color(0xFAFAF8)); }
            @Override public void mouseExited (MouseEvent e) { addCard.setBackground(CARD); inner.setBackground(CARD); }
        });
        gridPanel.add(addCard);

        gridPanel.revalidate();
        gridPanel.repaint();
    }

    private JPanel buildProjectCard(Project p) {
        RoundedPanel card = new RoundedPanel(12, BORDER);
        card.setBackground(CARD);
        card.setLayout(new BorderLayout(0, 0));
        card.setBorder(new EmptyBorder(18, 18, 18, 18));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Top row: icon + status badge
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setBackground(CARD);
        topRow.setBorder(new EmptyBorder(0, 0, 12, 0));

        Color projColor = parseHex(p.getColor(), SUCCESS);
        JLabel icon = new JLabel("⊞");
        icon.setFont(new Font("Arial", Font.PLAIN, 22));
        icon.setForeground(projColor);
        icon.setBackground(blend(projColor, Color.WHITE, 0.15f));
        icon.setOpaque(true);
        icon.setBorder(new EmptyBorder(8, 8, 8, 8));

        boolean done = p.getProgress() >= 100;
        JLabel badge = new JLabel(done ? I18n.t("projects.status.done")
                                       : I18n.t("projects.status.active"));
        badge.setFont(new Font("Arial", Font.BOLD, 10));
        badge.setForeground(done ? SUCCESS : new Color(0x0F6E56));
        badge.setBackground(done ? SUC_BG : SUC_BG);
        badge.setOpaque(true);
        badge.setBorder(new EmptyBorder(3, 9, 3, 9));

        // ⋯ menu button (archive)
        User me = AppContext.getInstance().getCurrentUser();
        boolean isOwner = me != null && me.getUserId() == p.getOwnerId();
        JLabel menuBtn = new JLabel("⋯");
        menuBtn.setFont(new Font("Arial", Font.PLAIN, 16));
        menuBtn.setForeground(SEC);
        menuBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        menuBtn.setVisible(isOwner);
        menuBtn.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                e.consume();
                JPopupMenu menu = new JPopupMenu();
                JMenuItem archiveItem = new JMenuItem(I18n.t("projects.archive"));
                archiveItem.setForeground(new Color(0xA32D2D));
                archiveItem.addActionListener(ae -> {
                    int choice = JOptionPane.showConfirmDialog(
                            ProjectsPanel.this,
                            I18n.t("projects.archive.confirm"),
                            I18n.t("projects.archive"),
                            JOptionPane.YES_NO_OPTION);
                    if (choice == JOptionPane.YES_OPTION) {
                        new SwingWorker<Boolean, Void>() {
                            @Override protected Boolean doInBackground() throws Exception {
                                var r = ServerConnection.getInstance()
                                        .send("ARCHIVE_PROJECT", p.getProjectId());
                                return r.isSuccess();
                            }
                            @Override protected void done() {
                                try {
                                    if (get()) {
                                        ToastManager.success(I18n.t("toast.project_archived"));
                                        load();
                                        if (onProjectsChanged != null) onProjectsChanged.run();
                                    }
                                } catch (Exception ignored) {}
                            }
                        }.execute();
                    }
                });
                menu.add(archiveItem);
                menu.show(menuBtn, 0, menuBtn.getHeight());
            }
            @Override public void mouseEntered(MouseEvent e) { menuBtn.setForeground(TEXT); }
            @Override public void mouseExited(MouseEvent e)  { menuBtn.setForeground(SEC); }
        });

        JPanel topRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        topRight.setBackground(CARD);
        topRight.add(badge);
        topRight.add(menuBtn);

        topRow.add(icon, BorderLayout.WEST);
        topRow.add(topRight, BorderLayout.EAST);
        card.add(topRow, BorderLayout.NORTH);

        // Body: name + desc + progress
        JPanel body = new JPanel();
        body.setBackground(CARD);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

        JLabel name = new JLabel(p.getName());
        name.setFont(new Font("Arial", Font.BOLD, 14));
        name.setForeground(TEXT);
        name.setAlignmentX(LEFT_ALIGNMENT);
        body.add(name);
        body.add(Box.createVerticalStrut(5));

        if (p.getDescription() != null && !p.getDescription().isBlank()) {
            JLabel desc = new JLabel("<html><body style='width:180px;color:#888780'>"
                    + escapeHtml(p.getDescription()) + "</body></html>");
            desc.setFont(new Font("Arial", Font.PLAIN, 11));
            desc.setAlignmentX(LEFT_ALIGNMENT);
            body.add(desc);
            body.add(Box.createVerticalStrut(14));
        }

        // Progress label
        JPanel progRow = new JPanel(new BorderLayout());
        progRow.setBackground(CARD);
        JLabel progLbl = new JLabel(I18n.t("projects.progress"));
        progLbl.setFont(new Font("Arial", Font.PLAIN, 11));
        progLbl.setForeground(SEC);
        JLabel progPct = new JLabel(Math.round(p.getProgress()) + "%");
        progPct.setFont(new Font("Arial", Font.BOLD, 11));
        progPct.setForeground(TEXT);
        progRow.add(progLbl, BorderLayout.WEST);
        progRow.add(progPct, BorderLayout.EAST);
        progRow.setAlignmentX(LEFT_ALIGNMENT);
        body.add(progRow);
        body.add(Box.createVerticalStrut(5));

        // Progress bar
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue((int) p.getProgress());
        bar.setForeground(projColor);
        bar.setBackground(BG);
        bar.setBorderPainted(false);
        bar.setPreferredSize(new Dimension(0, 5));
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 5));
        bar.setAlignmentX(LEFT_ALIGNMENT);
        body.add(bar);
        body.add(Box.createVerticalStrut(14));

        // Footer: members avatars + manage button + task count
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(CARD);
        footer.setAlignmentX(LEFT_ALIGNMENT);

        JPanel membersRow = new JPanel(new FlowLayout(FlowLayout.LEFT, -5, 0));
        membersRow.setBackground(CARD);
        if (p.getMembers() != null) {
            for (int i = 0; i < Math.min(4, p.getMembers().size()); i++) {
                var u = p.getMembers().get(i);
                Color av = avatarColor(u.getUserId());
                membersRow.add(new AvatarLabel(u.getInitials(), av, 22));
            }
        }
        // Small "+" manage button
        JLabel manageBtn = new JLabel("＋");
        manageBtn.setFont(new Font("Arial", Font.PLAIN, 13));
        manageBtn.setForeground(SEC);
        manageBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(1, 5, 1, 5)));
        manageBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        manageBtn.setToolTipText(I18n.t("projects.manage_members"));
        manageBtn.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                e.consume(); // don't trigger card click
                showMembersDialog(p);
            }
            @Override public void mouseEntered(MouseEvent e) { manageBtn.setForeground(TEXT); }
            @Override public void mouseExited (MouseEvent e) { manageBtn.setForeground(SEC); }
        });
        membersRow.add(Box.createHorizontalStrut(6));
        membersRow.add(manageBtn);

        JLabel taskCount = new JLabel(p.getTaskCount() + " " + I18n.t("projects.tasks"));
        taskCount.setFont(new Font("Arial", Font.PLAIN, 11));
        taskCount.setForeground(SEC);

        footer.add(membersRow, BorderLayout.WEST);
        footer.add(taskCount, BorderLayout.EAST);
        body.add(footer);

        card.add(body, BorderLayout.CENTER);

        // Click → set project + go to kanban
        card.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                AppContext.getInstance().setCurrentProject(p);
                showPage.accept("kanban");
            }
            @Override public void mouseEntered(MouseEvent e) { card.setBorderColor(new Color(0xB0B0AC)); }
            @Override public void mouseExited (MouseEvent e) { card.setBorderColor(BORDER); }
        });
        return card;
    }

    // ── Members management dialog ─────────────────────────────────────────────

    private void showMembersDialog(Project p) {
        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dlg = new JDialog(owner,
                I18n.t("projects.manage_members") + " — " + p.getName(),
                Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setSize(460, 520);
        dlg.setLocationRelativeTo(owner);
        dlg.setResizable(false);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(Color.WHITE);
        root.setBorder(new EmptyBorder(24, 28, 20, 28));

        JLabel title = new JLabel(I18n.t("projects.manage_members"));
        title.setFont(new Font("Arial", Font.BOLD, 16));
        title.setForeground(TEXT);
        title.setBorder(new EmptyBorder(0, 0, 16, 0));
        root.add(title, BorderLayout.NORTH);

        // Members list
        JPanel listPanel = new JPanel();
        listPanel.setBackground(Color.WHITE);
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));
        scroll.getVerticalScrollBar().setUnitIncrement(12);
        root.add(scroll, BorderLayout.CENTER);

        // Bottom: add member section
        JPanel addSection = new JPanel(new GridBagLayout());
        addSection.setBackground(Color.WHITE);
        addSection.setBorder(new EmptyBorder(14, 0, 0, 0));
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(0, 0, 0, 0);

        JLabel addLbl = new JLabel(I18n.t("projects.add_member").toUpperCase());
        addLbl.setFont(new Font("Arial", Font.BOLD, 10));
        addLbl.setForeground(SEC);
        addLbl.setBorder(new EmptyBorder(0, 0, 6, 0));
        gc.gridx = 0; gc.gridy = 0; gc.gridwidth = 2; gc.weightx = 1;
        addSection.add(addLbl, gc);

        JComboBox<User> userCombo = new JComboBox<>();
        userCombo.setFont(new Font("Arial", Font.PLAIN, 13));
        userCombo.setBackground(new Color(0xFAFAF8));
        userCombo.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index, boolean sel, boolean focus) {
                super.getListCellRendererComponent(list, value, index, sel, focus);
                if (value instanceof User u) {
                    setText(u.getFullName() != null && !u.getFullName().isBlank()
                            ? u.getFullName() + " (@" + u.getUsername() + ")"
                            : "@" + u.getUsername());
                }
                return this;
            }
        });
        gc.gridy = 1; gc.gridwidth = 1; gc.weightx = 1; gc.insets = new Insets(0, 0, 0, 8);
        addSection.add(userCombo, gc);

        JButton addBtn = new JButton(I18n.t("btn.add"));
        addBtn.setBackground(new Color(0x1A1A18));
        addBtn.setForeground(Color.WHITE);
        addBtn.setBorderPainted(false);
        addBtn.setFocusPainted(false);
        addBtn.setFont(new Font("Arial", Font.BOLD, 12));
        addBtn.setPreferredSize(new Dimension(80, 34));
        gc.gridx = 1; gc.weightx = 0; gc.insets = new Insets(0, 0, 0, 0);
        addSection.add(addBtn, gc);
        root.add(addSection, BorderLayout.SOUTH);

        dlg.setContentPane(root);

        // Load data: current members + all users
        new SwingWorker<Object[][], Void>() {
            @Override protected Object[][] doInBackground() throws Exception {
                var mr = ServerConnection.getInstance().send("GET_USERS",     p.getProjectId());
                var ar = ServerConnection.getInstance().send("GET_ALL_USERS", null);
                List<User> members  = mr.isSuccess() ? (List<User>) mr.getData() : List.of();
                List<User> allUsers = ar.isSuccess() ? (List<User>) ar.getData() : List.of();
                return new Object[][]{ members.toArray(new User[0]), allUsers.toArray(new User[0]) };
            }
            @Override protected void done() {
                try {
                    Object[][] data = get();
                    List<User> members  = List.of((User[]) (Object[]) data[0]);
                    List<User> allUsers = List.of((User[]) (Object[]) data[1]);
                    renderMembersList(listPanel, members, p, dlg);
                    populateAddCombo(userCombo, allUsers, members);
                } catch (Exception ignored) {}
            }
        }.execute();

        // Add button action
        addBtn.addActionListener(e -> {
            User selected = (User) userCombo.getSelectedItem();
            if (selected == null) return;
            addBtn.setEnabled(false);
            new SwingWorker<Boolean, Void>() {
                @Override protected Boolean doInBackground() throws Exception {
                    var r = ServerConnection.getInstance().send("ADD_PROJECT_MEMBER",
                            new int[]{p.getProjectId(), selected.getUserId()});
                    return r.isSuccess();
                }
                @Override protected void done() {
                    addBtn.setEnabled(true);
                    try {
                        if (get()) {
                            dlg.dispose();
                            ToastManager.success(I18n.t("toast.member_added"));
                            load(); // refresh project cards
                        }
                    } catch (Exception ignored) {}
                }
            }.execute();
        });

        dlg.setVisible(true);
    }

    private void renderMembersList(JPanel listPanel, List<User> members, Project p, JDialog dlg) {
        listPanel.removeAll();
        User currentUser = AppContext.getInstance().getCurrentUser();
        int ownerId = p.getOwnerId();

        for (User u : members) {
            JPanel row = new JPanel(new BorderLayout(10, 0));
            row.setBackground(Color.WHITE);
            row.setBorder(new EmptyBorder(10, 14, 10, 14));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

            // Avatar + name
            JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
            left.setBackground(Color.WHITE);
            Color av = avatarColor(u.getUserId());
            left.add(new AvatarLabel(u.getInitials(), av, 32));

            JPanel namePane = new JPanel();
            namePane.setBackground(Color.WHITE);
            namePane.setLayout(new BoxLayout(namePane, BoxLayout.Y_AXIS));
            JLabel name = new JLabel(u.getFullName() != null && !u.getFullName().isBlank()
                    ? u.getFullName() : u.getUsername());
            name.setFont(new Font("Arial", Font.BOLD, 13));
            name.setForeground(TEXT);
            JLabel uname = new JLabel("@" + u.getUsername());
            uname.setFont(new Font("Arial", Font.PLAIN, 11));
            uname.setForeground(SEC);
            namePane.add(name);
            namePane.add(uname);
            left.add(namePane);
            row.add(left, BorderLayout.WEST);

            // Owner badge or Remove button
            if (u.getUserId() == ownerId) {
                JLabel ownerBadge = new JLabel("Owner");
                ownerBadge.setFont(new Font("Arial", Font.BOLD, 10));
                ownerBadge.setForeground(new Color(0x0F6E56));
                ownerBadge.setBackground(new Color(0xE1F5EE));
                ownerBadge.setOpaque(true);
                ownerBadge.setBorder(new EmptyBorder(3, 8, 3, 8));
                row.add(ownerBadge, BorderLayout.EAST);
            } else if (currentUser != null &&
                       (currentUser.getUserId() == ownerId ||
                        currentUser.getRole() != null && currentUser.getRole().equals("admin"))) {
                JButton removeBtn = new JButton(I18n.t("btn.remove"));
                removeBtn.setFont(new Font("Arial", Font.PLAIN, 11));
                removeBtn.setForeground(new Color(0xA32D2D));
                removeBtn.setBackground(new Color(0xFEF2F2));
                removeBtn.setBorderPainted(false);
                removeBtn.setFocusPainted(false);
                removeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                removeBtn.addActionListener(e -> {
                    removeBtn.setEnabled(false);
                    new SwingWorker<Boolean, Void>() {
                        @Override protected Boolean doInBackground() throws Exception {
                            var r = ServerConnection.getInstance().send("REMOVE_PROJECT_MEMBER",
                                    new int[]{p.getProjectId(), u.getUserId()});
                            return r.isSuccess();
                        }
                        @Override protected void done() {
                            try {
                                if (get()) {
                                    dlg.dispose();
                                    load();
                                }
                            } catch (Exception ignored) {}
                        }
                    }.execute();
                });
                row.add(removeBtn, BorderLayout.EAST);
            }

            // Separator
            JPanel sep = new JPanel();
            sep.setBackground(BORDER);
            sep.setPreferredSize(new Dimension(0, 1));
            sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));

            listPanel.add(row);
            listPanel.add(sep);
        }

        if (members.isEmpty()) {
            JLabel empty = new JLabel(I18n.t("projects.no_members"), SwingConstants.CENTER);
            empty.setForeground(SEC);
            empty.setFont(new Font("Arial", Font.PLAIN, 13));
            empty.setAlignmentX(CENTER_ALIGNMENT);
            empty.setBorder(new EmptyBorder(30, 0, 30, 0));
            listPanel.add(empty);
        }

        listPanel.revalidate();
        listPanel.repaint();
    }

    private void populateAddCombo(JComboBox<User> combo, List<User> allUsers, List<User> existing) {
        Set<Integer> memberIds = new HashSet<>();
        for (User u : existing) memberIds.add(u.getUserId());
        combo.removeAllItems();
        for (User u : allUsers) {
            if (!memberIds.contains(u.getUserId())) combo.addItem(u);
        }
    }

    private Color parseHex(String hex, Color fallback) {
        try { return hex != null ? Color.decode(hex) : fallback; }
        catch (NumberFormatException e) { return fallback; }
    }

    private Color blend(Color c, Color base, float alpha) {
        int r = (int)(c.getRed()   * alpha + base.getRed()   * (1 - alpha));
        int g = (int)(c.getGreen() * alpha + base.getGreen() * (1 - alpha));
        int b = (int)(c.getBlue()  * alpha + base.getBlue()  * (1 - alpha));
        return new Color(r, g, b);
    }

    private Color avatarColor(int id) {
        Color[] pal = {new Color(0x0F6E56), new Color(0x7C3AED),
                       new Color(0x2563EB), new Color(0xD97706)};
        return pal[Math.abs(id) % pal.length];
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }
}
