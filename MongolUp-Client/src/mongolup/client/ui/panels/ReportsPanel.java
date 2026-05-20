package mongolup.client.ui.panels;

import mongolup.client.AppContext;
import mongolup.client.ServerConnection;
import mongolup.client.i18n.I18n;
import mongolup.client.ui.components.RoundedPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unchecked")
public class ReportsPanel extends JPanel {

    // ── Palette ──────────────────────────────────────────────────────────────
    private static final Color BG      = new Color(0xF5F5F3);
    private static final Color CARD    = Color.WHITE;
    private static final Color BORDER  = new Color(0xE8E8E5);
    private static final Color TEXT    = new Color(0x1A1A18);
    private static final Color SEC     = new Color(0x888780);
    private static final Color SUCCESS = new Color(0x0F6E56);
    private static final Color ERR     = new Color(0xD93025);
    private static final Color BLUE    = new Color(0x3B82F6);
    private static final Color AMBER   = new Color(0xF59E0B);
    private static final Color PURPLE  = new Color(0x8B5CF6);
    private static final Color MUTED   = new Color(0xD1D5DB);

    /** Colour palette rotated across statuses. */
    private static final Color[] STATUS_COLORS = { MUTED, BLUE, AMBER, SUCCESS, PURPLE };

    // ── State ────────────────────────────────────────────────────────────────
    private JPanel contentPanel;

    // ── Constructor ──────────────────────────────────────────────────────────
    public ReportsPanel() {
        setBackground(BG);
        setLayout(new BorderLayout());
        buildShell();
        I18n.onLocaleChange(this::load);
    }

    // ── Shell (header + scroll wrapper) ─────────────────────────────────────
    private void buildShell() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG);
        wrapper.setBorder(new EmptyBorder(28, 28, 28, 28));

        // Header
        JLabel title = new JLabel(I18n.t("reports.title"));
        title.setFont(new Font("Arial", Font.BOLD, 22));
        title.setForeground(TEXT);
        title.setBorder(new EmptyBorder(0, 0, 24, 0));
        wrapper.add(title, BorderLayout.NORTH);

        // Scrollable body
        contentPanel = new JPanel();
        contentPanel.setBackground(BG);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        JScrollPane scroll = new JScrollPane(contentPanel);
        scroll.setBorder(null);
        scroll.setBackground(BG);
        scroll.getViewport().setBackground(BG);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        wrapper.add(scroll, BorderLayout.CENTER);

        add(wrapper, BorderLayout.CENTER);
    }

    // ── Load ─────────────────────────────────────────────────────────────────
    public void load() {
        AppContext ctx = AppContext.getInstance();
        if (ctx.getCurrentProject() == null) {
            showPlaceholder(I18n.t("error.no_project"));
            return;
        }
        int projectId = ctx.getCurrentProject().getProjectId();

        new SwingWorker<List<Object[]>, Void>() {
            @Override protected List<Object[]> doInBackground() throws Exception {
                var r = ServerConnection.getInstance().send("GET_REPORT_DATA", projectId);
                return r.isSuccess() ? (List<Object[]>) r.getData() : List.of();
            }
            @Override protected void done() {
                try   { render(get()); }
                catch (Exception e) { showPlaceholder("Error: " + e.getMessage()); }
            }
        }.execute();
    }

    private void showPlaceholder(String msg) {
        contentPanel.removeAll();
        JLabel lbl = new JLabel(msg, SwingConstants.CENTER);
        lbl.setForeground(SEC);
        lbl.setFont(new Font("Arial", Font.PLAIN, 14));
        lbl.setAlignmentX(CENTER_ALIGNMENT);
        contentPanel.add(Box.createVerticalStrut(80));
        contentPanel.add(lbl);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    // ── Render ────────────────────────────────────────────────────────────────
    private void render(List<Object[]> rows) {
        contentPanel.removeAll();

        // Split out the overdue sentinel
        int overdue = 0;
        List<Object[]> statusRows = new ArrayList<>();
        for (Object[] r : rows) {
            if ("__overdue__".equals(r[0])) overdue = (int) r[1];
            else statusRows.add(r);
        }

        int total  = statusRows.stream().mapToInt(r -> (int) r[1]).sum();
        int done   = statusRows.stream()
                .filter(r -> { String n = ((String)r[0]).toLowerCase();
                               return n.contains("дуусс") || n.contains("done") || n.contains("finish"); })
                .mapToInt(r -> (int) r[1]).sum();
        int onTime = total - overdue;

        // ── Row 1: stat cards ────────────────────────────────────────────
        JPanel statsRow = new JPanel(new GridLayout(1, 4, 12, 0));
        statsRow.setBackground(BG);
        statsRow.setAlignmentX(LEFT_ALIGNMENT);
        statsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 112));
        statsRow.setPreferredSize(new Dimension(0, 112));

        String doneRate = total > 0
                ? Math.round(100.0 * done  / total) + I18n.t("reports.sub.pct_complete")
                : I18n.t("reports.sub.no_tasks");
        String onTmLbl  = total > 0
                ? onTime + " " + I18n.t("reports.sub.on_schedule")
                : I18n.t("reports.sub.no_data");
        String ovdLbl   = overdue > 0
                ? I18n.t("reports.sub.need_attention")
                : I18n.t("reports.sub.all_on_track");

        statsRow.add(statCard(I18n.t("reports.total_tasks"), String.valueOf(total),
                BLUE,    I18n.t("reports.sub.active"), null));
        statsRow.add(statCard(I18n.t("reports.done"),        String.valueOf(done),
                SUCCESS, doneRate,                      null));
        statsRow.add(statCard(I18n.t("reports.on_time"),
                total > 0 ? Math.round(100.0 * onTime / total) + "%" : "—",
                SUCCESS, onTmLbl, null));
        statsRow.add(statCard(I18n.t("reports.overdue"),     String.valueOf(overdue),
                overdue > 0 ? ERR : SUCCESS, ovdLbl, null));

        contentPanel.add(statsRow);
        contentPanel.add(Box.createVerticalStrut(14));

        // ── Row 2: chart cards ───────────────────────────────────────────
        JPanel chartsRow = new JPanel(new GridLayout(1, 2, 14, 0));
        chartsRow.setBackground(BG);
        chartsRow.setAlignmentX(LEFT_ALIGNMENT);
        chartsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));
        chartsRow.setPreferredSize(new Dimension(0, 300));

        chartsRow.add(buildStatusCard(statusRows, total));
        chartsRow.add(buildCompletionCard(done, total, overdue, onTime));

        contentPanel.add(chartsRow);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Stat card
    // ═══════════════════════════════════════════════════════════════════════

    private JPanel statCard(String label, String value, Color accent, String sub, Object ignored) {
        RoundedPanel card = new RoundedPanel(12, BORDER);
        card.setBackground(CARD);
        card.setLayout(new BorderLayout());

        // 4px accent strip on top (bleed into rounded corners via clip)
        JPanel strip = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(accent);
                // extend bottom so it meets flush with body below
                g2.fillRoundRect(0, 0, getWidth(), getHeight() + 12, 12, 12);
                g2.dispose();
            }
        };
        strip.setOpaque(false);
        strip.setPreferredSize(new Dimension(0, 5));
        card.add(strip, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(14, 18, 14, 18));

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Arial", Font.PLAIN, 11));
        lbl.setForeground(SEC);
        lbl.setAlignmentX(LEFT_ALIGNMENT);

        JLabel val = new JLabel(value);
        val.setFont(new Font("Arial", Font.BOLD, 30));
        val.setForeground(TEXT);
        val.setAlignmentX(LEFT_ALIGNMENT);

        JLabel subLbl = new JLabel(sub != null ? sub : " ");
        subLbl.setFont(new Font("Arial", Font.PLAIN, 11));
        subLbl.setForeground(accent);
        subLbl.setAlignmentX(LEFT_ALIGNMENT);

        body.add(lbl);
        body.add(Box.createVerticalStrut(5));
        body.add(val);
        body.add(Box.createVerticalStrut(3));
        body.add(subLbl);

        card.add(body, BorderLayout.CENTER);
        return card;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Status breakdown card (stacked pill bar + legend rows)
    // ═══════════════════════════════════════════════════════════════════════

    private JPanel buildStatusCard(List<Object[]> statusRows, int total) {
        RoundedPanel card = new RoundedPanel(12, BORDER);
        card.setBackground(CARD);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel(I18n.t("reports.by_status"));
        title.setFont(new Font("Arial", Font.BOLD, 14));
        title.setForeground(TEXT);
        card.add(title, BorderLayout.NORTH);

        JPanel inner = new JPanel();
        inner.setOpaque(false);
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setBorder(new EmptyBorder(14, 0, 0, 0));

        // ── Stacked pill bar ──────────────────────────────────────────────
        JPanel stackedBar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int h = getHeight(), w = getWidth(), r = h / 2;

                if (total == 0 || statusRows.isEmpty()) {
                    g2.setColor(BORDER);
                    g2.fillRoundRect(0, 0, w, h, r, r);
                } else {
                    // Clip to pill shape so segments stay inside
                    g2.setClip(new RoundRectangle2D.Float(0, 0, w, h, r, r));
                    int x = 0;
                    for (int i = 0; i < statusRows.size(); i++) {
                        int cnt  = (int) statusRows.get(i)[1];
                        int segW = (i == statusRows.size() - 1)
                                   ? w - x   // last segment fills remainder
                                   : (int) Math.round((double) cnt / total * w);
                        g2.setColor(STATUS_COLORS[i % STATUS_COLORS.length]);
                        g2.fillRect(x, 0, segW, h);
                        x += segW;
                    }
                }
                g2.dispose();
            }
        };
        stackedBar.setOpaque(false);
        stackedBar.setPreferredSize(new Dimension(0, 14));
        stackedBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 14));
        stackedBar.setAlignmentX(LEFT_ALIGNMENT);
        inner.add(stackedBar);
        inner.add(Box.createVerticalStrut(14));

        // ── Legend rows ───────────────────────────────────────────────────
        for (int i = 0; i < statusRows.size(); i++) {
            String name  = (String) statusRows.get(i)[0];
            int    cnt   = (int)    statusRows.get(i)[1];
            Color  col   = STATUS_COLORS[i % STATUS_COLORS.length];
            int    pct   = total > 0 ? (int) Math.round(100.0 * cnt / total) : 0;
            final int fp = pct;
            final Color fc = col;

            JPanel row = new JPanel(new BorderLayout(10, 0));
            row.setOpaque(false);
            row.setAlignmentX(LEFT_ALIGNMENT);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));

            // Dot + name
            JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            left.setOpaque(false);
            left.setPreferredSize(new Dimension(150, 22));

            JPanel dot = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(fc);
                    g2.fillOval(0, (getHeight() - 8) / 2, 8, 8);
                    g2.dispose();
                }
            };
            dot.setOpaque(false);
            dot.setPreferredSize(new Dimension(8, 8));

            JLabel nameLbl = new JLabel(name);
            nameLbl.setFont(new Font("Arial", Font.PLAIN, 12));
            nameLbl.setForeground(TEXT);

            left.add(dot);
            left.add(nameLbl);

            // Mini bar + pct
            JPanel barAndPct = new JPanel(new BorderLayout(6, 0));
            barAndPct.setOpaque(false);

            JPanel miniBar = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int bh = 6, by = (getHeight() - bh) / 2;
                    g2.setColor(BORDER);
                    g2.fillRoundRect(0, by, getWidth(), bh, 3, 3);
                    if (fp > 0) {
                        int fw = (int) (getWidth() * fp / 100.0);
                        g2.setColor(fc);
                        g2.fillRoundRect(0, by, fw, bh, 3, 3);
                    }
                    g2.dispose();
                }
            };
            miniBar.setOpaque(false);

            JLabel pctLbl = new JLabel(cnt + "");
            pctLbl.setFont(new Font("Arial", Font.BOLD, 11));
            pctLbl.setForeground(col);
            pctLbl.setPreferredSize(new Dimension(24, 18));

            barAndPct.add(miniBar, BorderLayout.CENTER);
            barAndPct.add(pctLbl, BorderLayout.EAST);

            row.add(left, BorderLayout.WEST);
            row.add(barAndPct, BorderLayout.CENTER);
            inner.add(row);
            inner.add(Box.createVerticalStrut(4));
        }

        if (statusRows.isEmpty()) {
            JLabel empty = new JLabel(I18n.t("kanban.no_tasks"), SwingConstants.CENTER);
            empty.setForeground(SEC);
            empty.setFont(new Font("Arial", Font.PLAIN, 13));
            empty.setAlignmentX(CENTER_ALIGNMENT);
            inner.add(empty);
        }

        card.add(inner, BorderLayout.CENTER);
        return card;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Completion ring card
    // ═══════════════════════════════════════════════════════════════════════

    private JPanel buildCompletionCard(int done, int total, int overdue, int onTime) {
        RoundedPanel card = new RoundedPanel(12, BORDER);
        card.setBackground(CARD);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel(I18n.t("reports.done"));
        title.setFont(new Font("Arial", Font.BOLD, 14));
        title.setForeground(TEXT);
        card.add(title, BorderLayout.NORTH);

        int pct = total > 0 ? (int) Math.round(100.0 * done / total) : 0;
        // Capture palette colours as local finals so they're usable inside the anonymous class
        // (ImageObserver.ERROR conflicts with our class field inside JPanel subclasses)
        final Color errColor = ERR;
        final Color successColor = SUCCESS;
        final Color borderColor = BORDER;
        final Color textColor = TEXT;
        final Color secColor = SEC;

        // ── Donut ring ────────────────────────────────────────────────────
        JPanel donut = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int size = Math.min(getWidth(), getHeight()) - 28;
                if (size < 40) { g2.dispose(); return; }
                int cx = (getWidth()  - size) / 2;
                int cy = (getHeight() - size) / 2;
                int sw = 20;
                int innerX = cx + sw / 2, innerY = cy + sw / 2;
                int innerW = size - sw,   innerH = size - sw;

                // Track
                g2.setColor(borderColor);
                g2.setStroke(new BasicStroke(sw, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawOval(innerX, innerY, innerW, innerH);

                // Overdue arc — drawn first (bottom layer)
                if (total > 0 && overdue > 0) {
                    double ovdAngle = 360.0 * overdue / total;
                    g2.setColor(new Color(errColor.getRed(), errColor.getGreen(), errColor.getBlue(), 180));
                    Arc2D ovdArc = new Arc2D.Double(innerX, innerY, innerW, innerH,
                            90 - (360.0 * done / Math.max(1,total)), -ovdAngle, Arc2D.OPEN);
                    g2.draw(ovdArc);
                }

                // Done arc
                if (pct > 0) {
                    g2.setColor(successColor);
                    Arc2D arc = new Arc2D.Double(innerX, innerY, innerW, innerH,
                            90, -(360.0 * pct / 100), Arc2D.OPEN);
                    g2.draw(arc);
                }

                // Center: big percentage
                g2.setColor(textColor);
                Font big = new Font("Arial", Font.BOLD, (int)(size * 0.22));
                g2.setFont(big);
                String pctStr = pct + "%";
                FontMetrics fmB = g2.getFontMetrics(big);
                int tx = (getWidth() - fmB.stringWidth(pctStr)) / 2;
                int midY = cy + size / 2;
                int ty = midY + fmB.getAscent() / 2 - 4;
                g2.drawString(pctStr, tx, ty);

                // Center: sub label
                g2.setColor(secColor);
                Font sub = new Font("Arial", Font.PLAIN, (int)(size * 0.085));
                g2.setFont(sub);
                String subStr = done + " / " + total;
                FontMetrics fmS = g2.getFontMetrics(sub);
                int sx = (getWidth() - fmS.stringWidth(subStr)) / 2;
                g2.drawString(subStr, sx, ty + fmB.getHeight() * 2 / 3);

                g2.dispose();
            }
        };
        donut.setOpaque(false);
        card.add(donut, BorderLayout.CENTER);

        // ── Two mini-stat boxes ───────────────────────────────────────────
        JPanel miniRow = new JPanel(new GridLayout(1, 2, 8, 0));
        miniRow.setOpaque(false);
        miniRow.setPreferredSize(new Dimension(0, 58));
        miniRow.setBorder(new EmptyBorder(10, 0, 0, 0));

        miniRow.add(miniStatBox(I18n.t("reports.on_time"),  String.valueOf(onTime),  SUCCESS));
        miniRow.add(miniStatBox(I18n.t("reports.overdue"),  String.valueOf(overdue), overdue > 0 ? ERR : SEC));

        card.add(miniRow, BorderLayout.SOUTH);
        return card;
    }

    private JPanel miniStatBox(String label, String value, Color valueColor) {
        RoundedPanel p = new RoundedPanel(8, BORDER);
        p.setBackground(new Color(0xFAFAF9));
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(8, 12, 8, 12));

        JLabel v = new JLabel(value);
        v.setFont(new Font("Arial", Font.BOLD, 18));
        v.setForeground(valueColor);
        v.setAlignmentX(LEFT_ALIGNMENT);

        JLabel l = new JLabel(label);
        l.setFont(new Font("Arial", Font.PLAIN, 10));
        l.setForeground(SEC);
        l.setAlignmentX(LEFT_ALIGNMENT);

        p.add(v);
        p.add(Box.createVerticalStrut(2));
        p.add(l);
        return p;
    }
}
