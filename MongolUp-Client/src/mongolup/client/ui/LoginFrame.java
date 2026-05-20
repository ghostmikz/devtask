package mongolup.client.ui;

import mongolup.client.AppContext;
import mongolup.client.ServerConnection;
import mongolup.client.i18n.I18n;
import mongolup.server.model.Response;
import mongolup.server.model.User;
import mongolup.server.model.LoginRequest;
import mongolup.server.model.RegisterRequest;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

public class LoginFrame extends JFrame {

    private static final Color ACCENT    = new Color(0x1A1A18);
    private static final Color BG        = new Color(0xF5F5F3);
    private static final Color CARD      = Color.WHITE;
    private static final Color BORDER    = new Color(0xE8E8E5);
    private static final Color TEXT_SEC  = new Color(0x888780);
    private static final Color SUCCESS   = new Color(0x0F6E56);
    private static final Color ERROR     = new Color(0xA32D2D);
    private static final Color INPUT_BG  = new Color(0xFAFAF8);

    // panels
    private JPanel loginPanel;
    private JPanel registerPanel;
    private CardLayout cardLayout;
    private JPanel cards;

    // login fields
    private JTextField  lUsername;
    private JPasswordField lPassword;
    private JLabel      lError;

    // register fields
    private JTextField  rFullName, rEmail, rUsername;
    private JPasswordField rPassword;
    private JLabel      rError;

    public LoginFrame() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setTitle(I18n.t("app.title"));
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setMinimumSize(new Dimension(600, 500));
        buildUI();
        I18n.onLocaleChange(this::refreshText);
    }

    private void buildUI() {
        getContentPane().setBackground(BG);
        setLayout(new GridBagLayout());   // centers the card both axes

        JPanel card = new JPanel();
        card.setBackground(CARD);
        card.setPreferredSize(new Dimension(440, 580));
        card.setMaximumSize(new Dimension(440, 580));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(36, 40, 36, 40)));
        card.setLayout(new BorderLayout());

        // Logo header
        JPanel header = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        header.setBackground(CARD);
        header.setBorder(new EmptyBorder(0, 0, 24, 0));
        JLabel dot = new JLabel("●");
        dot.setForeground(new Color(0x4ADE80));
        dot.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        JLabel logo = new JLabel("DevTask");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 20));
        logo.setForeground(ACCENT);
        header.add(dot);
        header.add(logo);
        card.add(header, BorderLayout.NORTH);

        // Card layout for login / register
        cardLayout = new CardLayout();
        cards = new JPanel(cardLayout);
        cards.setBackground(CARD);
        cards.add(buildLoginPanel(), "login");
        cards.add(buildRegisterPanel(), "register");
        card.add(cards, BorderLayout.CENTER);

        GridBagConstraints gbc = new GridBagConstraints();
        // NONE so the card keeps its preferred size and stays centered
        gbc.fill = GridBagConstraints.NONE;
        add(card, gbc);
    }

    // ── login panel ───────────────────────────────────────────────────────────

    private JPanel buildLoginPanel() {
        loginPanel = new JPanel();
        loginPanel.setBackground(CARD);
        loginPanel.setLayout(new BoxLayout(loginPanel, BoxLayout.Y_AXIS));

        JLabel title = sectionTitle(I18n.t("login.title"));
        loginPanel.add(title);
        loginPanel.add(Box.createVerticalStrut(20));

        lUsername = styledField("");
        loginPanel.add(fieldGroup(I18n.t("login.username"), lUsername));
        loginPanel.add(Box.createVerticalStrut(12));

        lPassword = new JPasswordField();
        styleInput(lPassword);
        loginPanel.add(fieldGroup(I18n.t("login.password"), lPassword));
        loginPanel.add(Box.createVerticalStrut(20));

        lError = new JLabel(" ");
        lError.setForeground(ERROR);
        lError.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lError.setAlignmentX(LEFT_ALIGNMENT);
        loginPanel.add(lError);
        loginPanel.add(Box.createVerticalStrut(6));

        JButton btnLogin = primaryButton(I18n.t("login.button"));
        btnLogin.addActionListener(e -> doLogin());
        loginPanel.add(btnLogin);
        loginPanel.add(Box.createVerticalStrut(14));

        JButton btnSwitch = linkButton(I18n.t("login.register.link"));
        btnSwitch.addActionListener(e -> cardLayout.show(cards, "register"));
        loginPanel.add(btnSwitch);

        // Enter key on password submits
        lPassword.addActionListener(e -> doLogin());
        return loginPanel;
    }

    // ── register panel ────────────────────────────────────────────────────────

    private JPanel buildRegisterPanel() {
        registerPanel = new JPanel();
        registerPanel.setBackground(CARD);
        registerPanel.setLayout(new BoxLayout(registerPanel, BoxLayout.Y_AXIS));

        JLabel title = sectionTitle(I18n.t("register.title"));
        registerPanel.add(title);
        registerPanel.add(Box.createVerticalStrut(16));

        rFullName = styledField("");
        registerPanel.add(fieldGroup(I18n.t("register.fullname"), rFullName));
        registerPanel.add(Box.createVerticalStrut(10));

        rEmail = styledField("");
        registerPanel.add(fieldGroup(I18n.t("register.email"), rEmail));
        registerPanel.add(Box.createVerticalStrut(10));

        rUsername = styledField("");
        registerPanel.add(fieldGroup(I18n.t("register.username"), rUsername));
        registerPanel.add(Box.createVerticalStrut(10));

        rPassword = new JPasswordField();
        styleInput(rPassword);
        registerPanel.add(fieldGroup(I18n.t("register.password"), rPassword));
        registerPanel.add(Box.createVerticalStrut(14));

        rError = new JLabel(" ");
        rError.setForeground(ERROR);
        rError.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        rError.setAlignmentX(LEFT_ALIGNMENT);
        registerPanel.add(rError);
        registerPanel.add(Box.createVerticalStrut(6));

        JButton btnReg = primaryButton(I18n.t("register.button"));
        btnReg.addActionListener(e -> doRegister());
        registerPanel.add(btnReg);
        registerPanel.add(Box.createVerticalStrut(14));

        JButton btnSwitch = linkButton(I18n.t("register.login.link"));
        btnSwitch.addActionListener(e -> cardLayout.show(cards, "login"));
        registerPanel.add(btnSwitch);

        rPassword.addActionListener(e -> doRegister());
        return registerPanel;
    }

    // ── actions ───────────────────────────────────────────────────────────────

    private void doLogin() {
        String username = lUsername.getText().trim();
        String password = new String(lPassword.getPassword());
        if (username.isEmpty() || password.isEmpty()) {
            lError.setText(I18n.t("login.error.empty"));
            return;
        }
        lError.setText(I18n.t("connecting"));

        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return ServerConnection.getInstance().send("LOGIN",
                        new LoginRequest(username, password));
            }
            @Override
            protected void done() {
                try {
                    Response r = get();
                    if (r.isSuccess()) {
                        User user = (User) r.getData();
                        AppContext.getInstance().setCurrentUser(user);
                        dispose();
                        new MainFrame().setVisible(true);
                    } else {
                        lError.setText(I18n.t("login.error.invalid"));
                    }
                } catch (Exception e) {
                    lError.setText(I18n.t("login.error.server"));
                }
            }
        }.execute();
    }

    private void doRegister() {
        String fullName  = rFullName.getText().trim();
        String email     = rEmail.getText().trim();
        String username  = rUsername.getText().trim();
        String password  = new String(rPassword.getPassword());

        if (fullName.isEmpty() || email.isEmpty() || username.isEmpty() || password.isEmpty()) {
            rError.setText(I18n.t("register.error.empty"));
            return;
        }
        rError.setText(I18n.t("connecting"));

        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return ServerConnection.getInstance().send("REGISTER",
                        new RegisterRequest(username, email, password, fullName));
            }
            @Override
            protected void done() {
                try {
                    Response r = get();
                    if (r.isSuccess()) {
                        User user = (User) r.getData();
                        AppContext.getInstance().setCurrentUser(user);
                        dispose();
                        new MainFrame().setVisible(true);
                    } else {
                        String msg = r.getMessage();
                        rError.setText(switch (msg != null ? msg : "") {
                            case "username_taken" -> I18n.t("register.error.username_taken");
                            case "email_taken"    -> I18n.t("register.error.email_taken");
                            default               -> msg;
                        });
                    }
                } catch (Exception e) {
                    rError.setText(I18n.t("login.error.server"));
                }
            }
        }.execute();
    }

    // ── locale refresh ────────────────────────────────────────────────────────

    private void refreshText() {
        setTitle(I18n.t("app.title"));
        // Rebuild panels in place is complex; simplest: dispose + reopen
        // For the login screen this is acceptable since no state is lost after logout
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private JLabel sectionTitle(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 18));
        l.setForeground(ACCENT);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private JTextField styledField(String placeholder) {
        JTextField tf = new JTextField(placeholder);
        styleInput(tf);
        return tf;
    }

    private void styleInput(JComponent c) {
        c.setBackground(INPUT_BG);
        c.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(8, 10, 8, 10)));
        c.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        c.setAlignmentX(LEFT_ALIGNMENT);
    }

    private JPanel fieldGroup(String label, JComponent input) {
        JPanel p = new JPanel();
        p.setBackground(CARD);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setAlignmentX(LEFT_ALIGNMENT);
        JLabel lbl = new JLabel(label.toUpperCase());
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lbl.setForeground(TEXT_SEC);
        lbl.setAlignmentX(LEFT_ALIGNMENT);
        p.add(lbl);
        p.add(Box.createVerticalStrut(4));
        p.add(input);
        return p;
    }

    private JButton primaryButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(ACCENT);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        b.setAlignmentX(LEFT_ALIGNMENT);
        return b;
    }

    private JButton linkButton(String text) {
        JButton b = new JButton("<html><u>" + text + "</u></html>");
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setForeground(new Color(0x2563EB));
        b.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setAlignmentX(LEFT_ALIGNMENT);
        return b;
    }
}
