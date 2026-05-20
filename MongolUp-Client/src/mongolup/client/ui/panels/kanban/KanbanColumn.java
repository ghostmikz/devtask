package mongolup.client.ui.panels.kanban;

import mongolup.client.i18n.I18n;
import mongolup.server.model.Status;
import mongolup.server.model.Task;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * A Kanban column: header + scrollable list of TaskCards.
 * Implements DropTarget to accept dragged cards.
 */
public class KanbanColumn extends JPanel implements DropTargetListener {

    private static final Color BG          = new Color(0xF5F5F3);
    private static final Color BORDER      = new Color(0xE8E8E5);
    private static final Color DRAG_OVER   = new Color(0xDDEEFF);
    private static final Color TEXT        = new Color(0x1A1A18);
    private static final Color SEC         = new Color(0x888780);
    private static final int   RADIUS      = 14;

    private boolean isDragOver = false;

    private final Status status;
    private final Consumer<Void> addTaskCallback;
    private final BiConsumer<Integer, Integer> onDrop; // (taskId, statusId)
    private final Consumer<Task> onCardClick;

    private JLabel countLabel;
    private JPanel cardsPanel;
    private final List<Task> tasks = new ArrayList<>();

    public KanbanColumn(Status status,
                        Consumer<Void> addTaskCallback,
                        BiConsumer<Integer, Integer> onDrop,
                        Consumer<Task> onCardClick) {
        this.status          = status;
        this.addTaskCallback = addTaskCallback;
        this.onDrop          = onDrop;
        this.onCardClick     = onCardClick;

        setOpaque(false);
        setBorder(new EmptyBorder(12, 10, 12, 10));
        setLayout(new BorderLayout(0, 8));

        buildHeader();
        buildCardsArea();
        buildAddButton();

        new DropTarget(this, DnDConstants.ACTION_MOVE, this, true);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(isDragOver ? DRAG_OVER : BG);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), RADIUS, RADIUS);
        g2.setColor(isDragOver ? new Color(0x93C5FD) : BORDER);
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, RADIUS, RADIUS);
        g2.dispose();
        super.paintComponent(g);
    }

    public Status getStatus() { return status; }

    public void setTasks(List<Task> tasks) {
        this.tasks.clear();
        this.tasks.addAll(tasks);
        refreshCards();
    }

    public void addTask(Task task) {
        tasks.removeIf(t -> t.getTaskId() == task.getTaskId());
        tasks.add(0, task);
        refreshCards();
    }

    public void removeTask(int taskId) {
        tasks.removeIf(t -> t.getTaskId() == taskId);
        refreshCards();
    }

    // ── build ─────────────────────────────────────────────────────────────────

    private void buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(0, 2, 6, 2));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);

        // color dot
        JLabel dot = new JLabel("●");
        dot.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        dot.setForeground(parseHex(status.getColor()));

        JLabel name = new JLabel(status.getName());
        name.setFont(new Font("Segoe UI", Font.BOLD, 12));
        name.setForeground(TEXT);

        countLabel = new JLabel("0");
        countLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        countLabel.setForeground(SEC);
        countLabel.setBackground(Color.WHITE);
        countLabel.setOpaque(true);
        countLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(1, 7, 1, 7)));

        left.add(dot);
        left.add(name);
        header.add(left, BorderLayout.WEST);
        header.add(countLabel, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
    }

    private void buildCardsArea() {
        cardsPanel = new JPanel();
        cardsPanel.setOpaque(false);
        cardsPanel.setLayout(new BoxLayout(cardsPanel, BoxLayout.Y_AXIS));

        JScrollPane scroll = new JScrollPane(cardsPanel);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scroll, BorderLayout.CENTER);
    }

    private void buildAddButton() {
        JPanel addPanel = new JPanel(new BorderLayout());
        addPanel.setOpaque(false);
        addPanel.setBorder(new EmptyBorder(4, 0, 0, 0));

        JLabel addBtn = new JLabel(I18n.t("kanban.add_task"), SwingConstants.CENTER);
        addBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        addBtn.setForeground(SEC);
        addBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createDashedBorder(BORDER, 4, 3),
                new EmptyBorder(8, 12, 8, 12)));
        addBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addBtn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                addTaskCallback.accept(null);
            }
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                addBtn.setForeground(TEXT);
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                addBtn.setForeground(SEC);
            }
        });

        addPanel.add(addBtn, BorderLayout.CENTER);
        add(addPanel, BorderLayout.SOUTH);
    }

    private void refreshCards() {
        cardsPanel.removeAll();
        for (Task t : tasks) {
            TaskCard card = new TaskCard(t, () -> onCardClick.accept(t));
            card.setAlignmentX(LEFT_ALIGNMENT);
            cardsPanel.add(card);
            cardsPanel.add(Box.createVerticalStrut(8));
        }
        countLabel.setText(String.valueOf(tasks.size()));
        cardsPanel.revalidate();
        cardsPanel.repaint();
    }

    // ── DropTarget ────────────────────────────────────────────────────────────

    @Override
    public void drop(DropTargetDropEvent dtde) {
        isDragOver = false;
        repaint();
        dtde.acceptDrop(DnDConstants.ACTION_MOVE);
        try {
            Transferable t = dtde.getTransferable();
            Task dragged = (Task) t.getTransferData(TaskCard.TASK_FLAVOR);
            if (dragged.getStatusId() != null && dragged.getStatusId() == status.getStatusId()) {
                dtde.dropComplete(false);
                return;
            }
            onDrop.accept(dragged.getTaskId(), status.getStatusId());
            dtde.dropComplete(true);
        } catch (Exception e) {
            dtde.dropComplete(false);
        }
    }

    @Override public void dragEnter(DropTargetDragEvent e) { isDragOver = true;  repaint(); }
    @Override public void dragExit(DropTargetEvent e)      { isDragOver = false; repaint(); }
    @Override public void dragOver(DropTargetDragEvent e)  {}
    @Override public void dropActionChanged(DropTargetDragEvent e) {}

    private Color parseHex(String hex) {
        try { return hex != null ? Color.decode(hex) : Color.GRAY; }
        catch (NumberFormatException e) { return Color.GRAY; }
    }
}
