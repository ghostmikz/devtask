package mongolup.client.ui.panels;

import mongolup.client.AppContext;
import mongolup.client.ServerConnection;
import mongolup.client.i18n.I18n;
import mongolup.server.model.User;
import mongolup.client.ui.components.AvatarLabel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class ProfilePanel extends JPanel {

    private static final Color BG       = new Color(0xF5F5F3);
    private static final Color CARD     = Color.WHITE;
    private static final Color BORDER   = new Color(0xE8E8E5);
    private static final Color TEXT     = new Color(0x1A1A18);
    private static final Color SEC      = new Color(0x888780);
    private static final Color ACCENT   = new Color(0x1A1A18);
    private static final Color SUCCESS  = new Color(0x0F6E56);
    private static final Color INPUT_BG = new Color(0xFAFAF8);

    // editable fields
    private JTextField nameField, emailField, phoneField, roleField;
    private JPasswordField newPwField, confirmPwField;
    private JLabel nameLabel;

    public ProfilePanel() {
        setBackground(BG);
        setLayout(new BorderLayout());
        buildUI();
        I18n.onLocaleChange(this::load);
    }

    private void buildUI() {
        JScrollPane scroll = new JScrollPane(buildContent());
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
    }

    private JPanel buildContent() {
        JPanel content = new JPanel(new BorderLayout(16, 0));
        content.setBackground(BG);
        content.setBorder(new EmptyBorder(18, 18, 18, 18));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG);
        header.setBorder(new EmptyBorder(0, 0, 18, 0));
        JLabel title = new JLabel(I18n.t("profile.title"));
        title.setFont(new Font("Arial", Font.BOLD, 20));
        title.setForeground(TEXT);
        JLabel subtitle = new JLabel(I18n.t("profile.subtitle"));
        subtitle.setFont(new Font("Arial", Font.PLAIN, 12));
        subtitle.setForeground(SEC);
        header.add(title, BorderLayout.NORTH);
        header.add(subtitle, BorderLayout.SOUTH);

        JPanel wrapper = new JPanel(new BorderLayout(0, 0));
        wrapper.setBackground(BG);
        wrapper.add(header, BorderLayout.NORTH);
        wrapper.add(buildLayout(), BorderLayout.CENTER);
        content.add(wrapper, BorderLayout.CENTER);
        return content;
    }

    private JPanel buildLayout() {
        JPanel layout = new JPanel(new GridBagLayout());
        layout.setBackground(BG);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 0, 0, 16);
        gbc.weighty = 1;

        // Left column: profile card + activity
        JPanel left = new JPanel();
        left.setBackground(BG);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(buildProfileCard());

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.3;
        layout.add(left, gbc);

        // Right column: form cards
        JPanel right = new JPanel();
        right.setBackground(BG);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.add(buildInfoCard());
        right.add(Box.createVerticalStrut(14));
        right.add(buildPasswordCard());

        gbc.gridx = 1; gbc.weightx = 0.7; gbc.insets = new Insets(0, 0, 0, 0);
        layout.add(right, gbc);

        return layout;
    }

    private JPanel buildProfileCard() {
        JPanel card = new JPanel();
        card.setBackground(CARD);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(24, 24, 24, 24)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 340));

        User u = AppContext.getInstance().getCurrentUser();
        String initials = u != null ? u.getInitials() : "?";
        AvatarLabel avatar = new AvatarLabel(initials, new Color(0x0F6E56), 84);
        avatar.setAlignmentX(CENTER_ALIGNMENT);
        card.add(avatar);
        card.add(Box.createVerticalStrut(14));

        nameLabel = new JLabel(u != null && u.getFullName() != null ? u.getFullName() : "");
        nameLabel.setFont(new Font("Arial", Font.BOLD, 19));
        nameLabel.setForeground(TEXT);
        nameLabel.setAlignmentX(CENTER_ALIGNMENT);
        card.add(nameLabel);
        card.add(Box.createVerticalStrut(4));

        JLabel role = new JLabel("Lead Developer · DevTask");
        role.setFont(new Font("Arial", Font.PLAIN, 12));
        role.setForeground(SEC);
        role.setAlignmentX(CENTER_ALIGNMENT);
        card.add(role);
        card.add(Box.createVerticalStrut(18));

        card.add(Box.createRigidArea(new Dimension(0, 14)));

        // Buttons
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        btns.setBackground(CARD);
        btns.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        btns.setAlignmentX(CENTER_ALIGNMENT);
        JButton edit = primaryBtn(I18n.t("profile.edit"));
        edit.setPreferredSize(new Dimension(110, 36));
        edit.addActionListener(e -> startEdit());
        JButton logout = secondaryBtn(I18n.t("profile.logout"));
        logout.setPreferredSize(new Dimension(110, 36));
        logout.setForeground(new Color(0xA32D2D));
        logout.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xFCA5A5)),
                new EmptyBorder(7, 13, 7, 13)));
        logout.addActionListener(e -> doLogout());
        btns.add(edit);
        btns.add(logout);
        card.add(btns);

        return card;
    }

    private JPanel buildInfoCard() {
        JPanel card = new JPanel();
        card.setBackground(CARD);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(20, 20, 20, 20)));
        card.setAlignmentX(LEFT_ALIGNMENT);

        card.add(sectionTitle(I18n.t("profile.info")));
        card.add(Box.createVerticalStrut(14));

        User u = AppContext.getInstance().getCurrentUser();
        nameField  = styledField(u != null && u.getFullName() != null ? u.getFullName() : "");
        emailField = styledField(u != null && u.getEmail() != null    ? u.getEmail()    : "");
        phoneField = styledField("+976");
        roleField  = styledField(u != null && u.getRole() != null     ? u.getRole()     : "member");
        nameField.setEditable(false);
        emailField.setEditable(false);

        JPanel row1 = twoCol(
                fieldGroup(I18n.t("profile.firstname"), nameField),
                fieldGroup(I18n.t("profile.email"),     emailField));
        JPanel row2 = twoCol(
                fieldGroup(I18n.t("profile.phone"),     phoneField),
                fieldGroup(I18n.t("profile.role"),      roleField));
        card.add(row1);
        card.add(Box.createVerticalStrut(10));
        card.add(row2);
        card.add(Box.createVerticalStrut(16));

        JButton save = primaryBtn(I18n.t("btn.save"));
        save.setAlignmentX(LEFT_ALIGNMENT);
        save.addActionListener(e -> saveProfile());
        card.add(save);
        return card;
    }

    private JPanel buildPasswordCard() {
        JPanel card = new JPanel();
        card.setBackground(CARD);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(20, 20, 20, 20)));
        card.setAlignmentX(LEFT_ALIGNMENT);
        card.add(sectionTitle(I18n.t("profile.new_password")));
        card.add(Box.createVerticalStrut(14));

        newPwField     = new JPasswordField(); styleInput(newPwField);
        confirmPwField = new JPasswordField(); styleInput(confirmPwField);

        card.add(twoCol(
                fieldGroup(I18n.t("profile.new_password"), newPwField),
                fieldGroup(I18n.t("profile.confirm_password"), confirmPwField)));
        card.add(Box.createVerticalStrut(16));

        JButton changePw = primaryBtn(I18n.t("btn.save"));
        changePw.setAlignmentX(LEFT_ALIGNMENT);
        changePw.addActionListener(e -> changePassword());
        card.add(changePw);
        return card;
    }

    // ── actions ───────────────────────────────────────────────────────────────

    public void load() {
        User u = AppContext.getInstance().getCurrentUser();
        if (u != null) {
            if (nameField  != null) nameField.setText(u.getFullName() != null ? u.getFullName() : "");
            if (emailField != null) emailField.setText(u.getEmail()   != null ? u.getEmail()    : "");
            if (nameLabel  != null) nameLabel.setText(u.getFullName() != null ? u.getFullName() : "");
        }
    }

    public void startEdit() {
        if (nameField  != null) nameField.setEditable(true);
        if (emailField != null) emailField.setEditable(true);
    }

    private void saveProfile() {
        if (nameField == null) return;
        User update = new User();
        update.setFullName(nameField.getText().trim());
        update.setEmail(emailField.getText().trim());
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                ServerConnection.getInstance().send("UPDATE_PROFILE", update);
                return null;
            }
            @Override protected void done() {
                User u = AppContext.getInstance().getCurrentUser();
                if (u != null) {
                    u.setFullName(update.getFullName());
                    u.setEmail(update.getEmail());
                }
                nameField.setEditable(false);
                emailField.setEditable(false);
                JOptionPane.showMessageDialog(ProfilePanel.this, I18n.t("btn.save") + " ✓");
            }
        }.execute();
    }

    private void changePassword() {
        String np = new String(newPwField.getPassword());
        String cp = new String(confirmPwField.getPassword());
        if (!np.equals(cp) || np.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Passwords don't match");
            return;
        }
        String old = JOptionPane.showInputDialog(this, "Current password:");
        if (old == null) return;
        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() throws Exception {
                var r = ServerConnection.getInstance()
                        .send("CHANGE_PASSWORD", new String[]{old, np});
                return r.isSuccess();
            }
            @Override protected void done() {
                try {
                    if (get()) {
                        newPwField.setText("");
                        confirmPwField.setText("");
                        JOptionPane.showMessageDialog(ProfilePanel.this, "Password changed ✓");
                    } else {
                        JOptionPane.showMessageDialog(ProfilePanel.this, "Wrong current password");
                    }
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void doLogout() {
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                ServerConnection.getInstance().send("LOGOUT", null);
                return null;
            }
            @Override protected void done() {
                AppContext.getInstance().clear();
                ServerConnection.getInstance().disconnect();
                Window w = SwingUtilities.getWindowAncestor(ProfilePanel.this);
                if (w != null) w.dispose();
                // Re-open login (reconnect needed)
                JOptionPane.showMessageDialog(null, "Logged out. Restart to reconnect.");
                System.exit(0);
            }
        }.execute();
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private JPanel statCell(String val, String label) {
        JPanel p = new JPanel();
        p.setBackground(CARD);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(10, 8, 10, 8)));
        p.setPreferredSize(new Dimension(70, 90));
        JLabel v = new JLabel(val, SwingConstants.CENTER);
        v.setFont(new Font("Arial", Font.BOLD, 22));
        v.setForeground(TEXT);
        v.setAlignmentX(CENTER_ALIGNMENT);
        JLabel l = new JLabel(label, SwingConstants.CENTER);
        l.setFont(new Font("Arial", Font.PLAIN, 10));
        l.setForeground(SEC);
        l.setAlignmentX(CENTER_ALIGNMENT);
        l.setBorder(new EmptyBorder(4, 0, 0, 0));
        p.add(v);
        p.add(l);
        return p;
    }

    private JLabel sectionTitle(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Arial", Font.BOLD, 14));
        l.setForeground(TEXT);
        l.setAlignmentX(LEFT_ALIGNMENT);
        l.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                new EmptyBorder(0, 0, 10, 0)));
        l.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        return l;
    }

    private JTextField styledField(String text) {
        JTextField tf = new JTextField(text);
        styleInput(tf);
        return tf;
    }

    private void styleInput(JComponent c) {
        c.setBackground(INPUT_BG);
        c.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(8, 10, 8, 10)));
        c.setFont(new Font("Arial", Font.PLAIN, 13));
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        c.setAlignmentX(LEFT_ALIGNMENT);
    }

    private JPanel fieldGroup(String label, JComponent input) {
        JPanel p = new JPanel();
        p.setBackground(CARD);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setAlignmentX(LEFT_ALIGNMENT);
        JLabel lbl = new JLabel(label.toUpperCase());
        lbl.setFont(new Font("Arial", Font.BOLD, 10));
        lbl.setForeground(SEC);
        lbl.setAlignmentX(LEFT_ALIGNMENT);
        p.add(lbl);
        p.add(Box.createVerticalStrut(4));
        p.add(input);
        return p;
    }

    private JPanel twoCol(JPanel a, JPanel b) {
        JPanel row = new JPanel(new GridLayout(1, 2, 12, 0));
        row.setBackground(CARD);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        row.add(a);
        row.add(b);
        return row;
    }

    private JButton primaryBtn(String text) {
        JButton b = new JButton(text);
        b.setBackground(ACCENT);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("Arial", Font.BOLD, 12));
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(8, 14, 8, 14));
        return b;
    }

    private JButton secondaryBtn(String text) {
        JButton b = new JButton(text);
        b.setBackground(CARD);
        b.setForeground(TEXT);
        b.setFont(new Font("Arial", Font.PLAIN, 12));
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(7, 13, 7, 13)));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

}
