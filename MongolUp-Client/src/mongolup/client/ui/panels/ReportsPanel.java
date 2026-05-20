package mongolup.client.ui.panels;

import mongolup.client.AppContext;
import mongolup.client.ServerConnection;
import mongolup.client.i18n.I18n;
import mongolup.server.model.Task;
import mongolup.client.ui.components.RoundedPanel;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

@SuppressWarnings("unchecked")
public class ReportsPanel extends JPanel {

    private static final Color BG      = new Color(0xF5F5F3);
    private static final Color CARD    = Color.WHITE;
    private static final Color BORDER  = new Color(0xE8E8E5);
    private static final Color TEXT    = new Color(0x1A1A18);
    private static final Color SEC     = new Color(0x888780);
    private static final Color SUCCESS = new Color(0x0F6E56);
    private static final Color ERROR   = new Color(0xA32D2D);
    private static final Color BLUE    = new Color(0x3B82F6);
    private static final Color AMBER   = new Color(0xF59E0B);

    private JPanel statsGrid;
    private JPanel chartsRow;

    public ReportsPanel() {
        setBackground(BG);
        setLayout(new BorderLayout());
        buildUI();
        I18n.onLocaleChange(this::load);
    }

    private void buildUI() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 0));
        wrapper.setBackground(BG);
        wrapper.setBorder(new EmptyBorder(22, 22, 22, 22));

        JLabel title = new JLabel(I18n.t("reports.title"));
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(TEXT);
        title.setBorder(new EmptyBorder(0, 0, 18, 0));
        wrapper.add(title, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setBackground(BG);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

        statsGrid = new JPanel(new GridLayout(1, 4, 12, 0));
        statsGrid.setBackground(BG);
        statsGrid.setAlignmentX(LEFT_ALIGNMENT);
        statsGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        body.add(statsGrid);
        body.add(Box.createVerticalStrut(14));

        chartsRow = new JPanel(new GridLayout(1, 2, 12, 0));
        chartsRow.setBackground(BG);
        chartsRow.setAlignmentX(LEFT_ALIGNMENT);
        chartsRow.setPreferredSize(new Dimension(0, 280));
        chartsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));
        body.add(chartsRow);

        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        wrapper.add(scroll, BorderLayout.CENTER);

        add(wrapper, BorderLayout.CENTER);
    }

    public void load() {
        AppContext ctx = AppContext.getInstance();
        if (ctx.getCurrentProject() == null) {
            showNoProject();
            return;
        }
        int projectId = ctx.getCurrentProject().getProjectId();

        new SwingWorker<List<Object[]>, Void>() {
            @Override protected List<Object[]> doInBackground() throws Exception {
                var r = ServerConnection.getInstance().send("GET_REPORT_DATA", projectId);
                return r.isSuccess() ? (List<Object[]>) r.getData() : List.of();
            }
            @Override protected void done() {
                try {
                    List<Object[]> rows = get();
                    renderStats(rows);
                    renderCharts(rows);
                } catch (Exception e) {
                    showError(e.getMessage());
                }
            }
        }.execute();
    }

    private void showNoProject() {
        statsGrid.removeAll();
        chartsRow.removeAll();
        JLabel lbl = new JLabel(I18n.t("error.no_project"), SwingConstants.CENTER);
        lbl.setForeground(SEC);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        statsGrid.setLayout(new BorderLayout());
        statsGrid.add(lbl, BorderLayout.CENTER);
        statsGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        statsGrid.revalidate();
        statsGrid.repaint();
    }

    private void showError(String msg) {
        statsGrid.removeAll();
        JLabel lbl = new JLabel("Error: " + msg, SwingConstants.CENTER);
        lbl.setForeground(ERROR);
        statsGrid.add(lbl);
        statsGrid.revalidate();
    }

    private void renderStats(List<Object[]> rows) {
        statsGrid.setLayout(new GridLayout(1, 4, 12, 0));
        statsGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        statsGrid.removeAll();
        int total = rows.stream().mapToInt(r -> (int) r[1]).sum();
        int done  = rows.stream()
                .filter(r -> ((String) r[0]).toLowerCase().contains("дуусс") ||
                             ((String) r[0]).toLowerCase().contains("done"))
                .mapToInt(r -> (int) r[1]).sum();
        int overdue = 0; // would need more data for real overdue count

        statsGrid.add(statCard(I18n.t("reports.total_tasks"), String.valueOf(total), "↑ 15%", SUCCESS));
        statsGrid.add(statCard(I18n.t("reports.done"),        String.valueOf(done),  "↑ 35%", SUCCESS));
        statsGrid.add(statCard(I18n.t("reports.on_time"),
                total > 0 ? Math.round(100.0 * done / total) + "%" : "—", "↑ 8%", SUCCESS));
        statsGrid.add(statCard(I18n.t("reports.overdue"), String.valueOf(overdue), "", ERROR));

        statsGrid.revalidate();
        statsGrid.repaint();
    }

    private void renderCharts(List<Object[]> rows) {
        chartsRow.removeAll();

        // Bar chart — weekly performance (static demo data matching HTML)
        DefaultCategoryDataset barDs = new DefaultCategoryDataset();
        String[] days = {"Да", "Мя", "Лх", "Пү", "Ба", "Бя", "Ня"};
        int[]    vals = {6, 8, 5, 9, 7, 3, 5};
        for (int i = 0; i < days.length; i++) {
            barDs.addValue(vals[i], I18n.t("reports.weekly_perf"), days[i]);
        }
        JFreeChart bar = ChartFactory.createBarChart(
                I18n.t("reports.weekly_perf"), "", "",
                barDs, PlotOrientation.VERTICAL, false, false, false);
        bar.setBackgroundPaint(CARD);
        bar.getPlot().setBackgroundPaint(CARD);
        bar.getPlot().setOutlinePaint(BORDER);
        ChartPanel barPanel = new ChartPanel(bar);
        barPanel.setBackground(CARD);
        barPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(16, 16, 16, 16)));
        chartsRow.add(barPanel);

        // Pie / donut chart — by status
        DefaultPieDataset<String> pieDs = new DefaultPieDataset<>();
        for (Object[] row : rows) {
            String name = (String) row[0];
            int    cnt  = (int)   row[1];
            if (cnt > 0) pieDs.setValue(name, cnt);
        }
        JFreeChart pie = ChartFactory.createPieChart(
                I18n.t("reports.by_status"), pieDs, true, false, false);
        pie.setBackgroundPaint(CARD);
        PiePlot<?> plot = (PiePlot<?>) pie.getPlot();
        plot.setBackgroundPaint(CARD);
        plot.setOutlinePaint(BORDER);
        // color each slice
        Color[] palette = {new Color(0xE8E8E5), BLUE, AMBER, SUCCESS};
        int ci = 0;
        for (Object[] row : rows) {
            if ((int) row[1] > 0) plot.setSectionPaint((String) row[0], palette[ci++ % palette.length]);
        }
        ChartPanel piePanel = new ChartPanel(pie);
        piePanel.setBackground(CARD);
        piePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(16, 16, 16, 16)));
        chartsRow.add(piePanel);

        chartsRow.revalidate();
        chartsRow.repaint();
    }

    private JPanel statCard(String label, String value, String change, Color changeColor) {
        RoundedPanel card = new RoundedPanel(10, BORDER);
        card.setBackground(CARD);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lbl.setForeground(SEC);
        lbl.setAlignmentX(LEFT_ALIGNMENT);

        JLabel val = new JLabel(value);
        val.setFont(new Font("Segoe UI", Font.BOLD, 26));
        val.setForeground(TEXT);
        val.setAlignmentX(LEFT_ALIGNMENT);

        JLabel chg = new JLabel(change);
        chg.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        chg.setForeground(changeColor);
        chg.setAlignmentX(LEFT_ALIGNMENT);

        card.add(lbl);
        card.add(Box.createVerticalStrut(6));
        card.add(val);
        card.add(Box.createVerticalStrut(4));
        card.add(chg);
        return card;
    }
}
