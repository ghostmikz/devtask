package mongolup.client.ui.panels;

import mongolup.client.i18n.I18n;
import mongolup.client.ui.components.RoundedPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Application settings panel — language, appearance, and about.
 */
public class SettingsPanel extends JPanel {

    private static final Color BG     = new Color(0xF5F5F3);
    private static final Color CARD   = Color.WHITE;
    private static final Color BORDER = new Color(0xE8E8E5);
    private static final Color TEXT   = new Color(0x1A1A18);
    private static final Color SEC    = new Color(0x888780);
    private static final Color ACCENT = new Color(0x1A1A18);

    public SettingsPanel() {
        setBackground(BG);
        setLayout(new BorderLayout());
        buildUI();
        I18n.onLocaleChange(this::rebuild);
    }

    private void buildUI() {
        removeAll();

        JPanel wrapper = new JPanel(new BorderLayout(0, 0));
        wrapper.setBackground(BG);
        wrapper.setBorder(new EmptyBorder(24, 24, 24, 24));

        JLabel title = new JLabel(I18n.t("nav.settings"));
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(TEXT);
        title.setBorder(new EmptyBorder(0, 0, 20, 0));
        wrapper.add(title, BorderLayout.NORTH);

        // Scrollable body
        JPanel body = new JPanel();
        body.setBackground(BG);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

        // ── Language card ─────────────────────────────────────────────────────
        body.add(sectionCard(I18n.t("settings.language"), buildLanguageSection()));
        body.add(Box.createVerticalStrut(14));

        // ── Appearance card ───────────────────────────────────────────────────
        body.add(sectionCard(I18n.t("settings.appearance"), buildAppearanceSection()));
        body.add(Box.createVerticalStrut(14));

        // ── About card ────────────────────────────────────────────────────────
        body.add(sectionCard(I18n.t("settings.about"), buildAboutSection()));

        JPanel topAlign = new JPanel(new BorderLayout());
        topAlign.setBackground(BG);
        topAlign.add(body, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(topAlign);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        wrapper.add(scroll, BorderLayout.CENTER);

        add(wrapper, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private void rebuild() { buildUI(); }

    // ── Section content builders ──────────────────────────────────────────────

    private JPanel buildLanguageSection() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        p.setBackground(CARD);

        ButtonGroup grp = new ButtonGroup();
        String current = I18n.getCurrentLang();

        JRadioButton en = langRadio("English", "en", current, grp, p);
        JRadioButton mn = langRadio("Монгол", "mn", current, grp, p);

        p.add(en);
        p.add(mn);
        return p;
    }

    private JRadioButton langRadio(String label, String langCode, String current,
                                   ButtonGroup grp, JPanel parent) {
        JRadioButton rb = new JRadioButton(label, langCode.equals(current));
        rb.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        rb.setForeground(TEXT);
        rb.setBackground(CARD);
        rb.setFocusPainted(false);
        rb.addActionListener(e -> {
            if (!I18n.getCurrentLang().equals(langCode)) {
                I18n.setLocale(langCode);
            }
        });
        grp.add(rb);
        return rb;
    }

    private JPanel buildAppearanceSection() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        p.setBackground(CARD);

        ButtonGroup grp = new ButtonGroup();
        JRadioButton light = new JRadioButton(I18n.t("settings.theme.light"), true);
        light.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        light.setForeground(TEXT);
        light.setBackground(CARD);
        light.setFocusPainted(false);

        JRadioButton dark = new JRadioButton(I18n.t("settings.theme.dark"));
        dark.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        dark.setForeground(SEC);
        dark.setBackground(CARD);
        dark.setFocusPainted(false);
        dark.setEnabled(false); // not yet implemented

        JLabel soon = new JLabel("  (" + I18n.t("settings.coming_soon") + ")");
        soon.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        soon.setForeground(SEC);

        grp.add(light);
        grp.add(dark);
        p.add(light);
        p.add(dark);
        p.add(soon);
        return p;
    }

    private JPanel buildAboutSection() {
        JPanel p = new JPanel();
        p.setBackground(CARD);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        p.add(aboutRow("MongolUp", "v1.0.0"));
        p.add(Box.createVerticalStrut(6));
        p.add(aboutRow(I18n.t("settings.about.tech"), "Java 17 · Swing · MySQL · FlatLaf"));
        p.add(Box.createVerticalStrut(6));
        p.add(aboutRow(I18n.t("settings.about.author"), "ghostmikz"));
        p.add(Box.createVerticalStrut(6));
        p.add(aboutRow("GitHub", "github.com/ghostmikz/devtask"));
        return p;
    }

    private JPanel aboutRow(String key, String value) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setBackground(CARD);
        row.setAlignmentX(LEFT_ALIGNMENT);
        JLabel k = new JLabel(key + ":  ");
        k.setFont(new Font("Segoe UI", Font.BOLD, 12));
        k.setForeground(SEC);
        JLabel v = new JLabel(value);
        v.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        v.setForeground(TEXT);
        row.add(k);
        row.add(v);
        return row;
    }

    // ── Reusable card wrapper ─────────────────────────────────────────────────

    private JPanel sectionCard(String heading, JPanel content) {
        RoundedPanel card = new RoundedPanel(12, BORDER);
        card.setBackground(CARD);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(18, 20, 18, 20));
        card.setAlignmentX(LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));

        JLabel h = new JLabel(heading);
        h.setFont(new Font("Segoe UI", Font.BOLD, 13));
        h.setForeground(TEXT);
        h.setBorder(new EmptyBorder(0, 0, 4, 0));

        JSeparator sep = new JSeparator();
        sep.setForeground(BORDER);

        JPanel top = new JPanel(new BorderLayout(0, 6));
        top.setBackground(CARD);
        top.add(h, BorderLayout.NORTH);
        top.add(sep, BorderLayout.CENTER);

        card.add(top, BorderLayout.NORTH);
        card.add(content, BorderLayout.CENTER);
        return card;
    }
}
