package mongolup.client.ui;

import mongolup.client.AppContext;
import mongolup.client.ServerConnection;
import mongolup.client.i18n.I18n;
import mongolup.server.model.Project;
import mongolup.server.model.Response;
import mongolup.server.model.Sprint;
import mongolup.client.ui.panels.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.List;

@SuppressWarnings("unchecked")
public class MainFrame extends JFrame {

    private static final Color BG = new Color(0xF5F5F3);

    private SidebarPanel sidebar;
    private TopbarPanel  topbar;
    private JPanel       contentArea;
    private CardLayout   cardLayout;

    private KanbanPanel   kanbanPanel;
    private MyTasksPanel  tasksPanel;
    private ReportsPanel  reportsPanel;
    private ProjectsPanel projectsPanel;
    private ProfilePanel  profilePanel;
    private SettingsPanel settingsPanel;

    public MainFrame() {
        setTitle(I18n.t("app.title"));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1200, 750);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
        loadAppIcon(this);
        buildUI();
        loadProjects();
        I18n.onLocaleChange(() -> setTitle(I18n.t("app.title")));
    }

    private void buildUI() {
        getContentPane().setLayout(new BorderLayout());
        getContentPane().setBackground(BG);

        // Sidebar
        sidebar = new SidebarPanel(this::showPage);
        add(sidebar, BorderLayout.WEST);

        // Main area (topbar + content)
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(BG);

        topbar = new TopbarPanel(this::handleAdd, null);
        main.add(topbar, BorderLayout.NORTH);

        // Card-switched content panels
        cardLayout   = new CardLayout();
        contentArea  = new JPanel(cardLayout);
        contentArea.setBackground(BG);

        kanbanPanel   = new KanbanPanel();
        tasksPanel    = new MyTasksPanel();
        reportsPanel  = new ReportsPanel();
        projectsPanel = new ProjectsPanel(this::showPage);
        projectsPanel.setOnProjectsChanged(this::loadProjects);
        profilePanel  = new ProfilePanel();
        settingsPanel = new SettingsPanel();

        contentArea.add(kanbanPanel,   "kanban");
        contentArea.add(tasksPanel,    "tasks");
        contentArea.add(reportsPanel,  "reports");
        contentArea.add(projectsPanel, "projects");
        contentArea.add(profilePanel,  "profile");
        contentArea.add(settingsPanel, "settings");

        main.add(contentArea, BorderLayout.CENTER);
        add(main, BorderLayout.CENTER);

        // Reload active sprint badge whenever the current project changes
        AppContext.getInstance().addProjectChangeListener(() -> {
            Project p = AppContext.getInstance().getCurrentProject();
            if (p == null) { topbar.setActiveSprint(null); return; }
            new SwingWorker<Sprint, Void>() {
                @Override protected Sprint doInBackground() throws Exception {
                    var r = ServerConnection.getInstance().send("GET_ACTIVE_SPRINT", p.getProjectId());
                    return r.isSuccess() && r.getData() instanceof Sprint s ? s : null;
                }
                @Override protected void done() {
                    try { topbar.setActiveSprint(get()); }
                    catch (Exception ignored) { topbar.setActiveSprint(null); }
                }
            }.execute();
        });

        // Show kanban by default
        showPage("kanban");
    }

    public void showPage(String pageId) {
        cardLayout.show(contentArea, pageId);
        String label = switch (pageId) {
            case "kanban"    -> I18n.t("nav.kanban");
            case "tasks"     -> I18n.t("nav.mytasks");
            case "reports"   -> I18n.t("nav.reports");
            case "projects"  -> I18n.t("nav.projects");
            case "profile"   -> I18n.t("nav.profile");
            case "settings"  -> I18n.t("nav.settings");
            default -> pageId;
        };
        topbar.setPage(pageId, label);

        // Lazy-load panel data
        switch (pageId) {
            case "kanban"   -> kanbanPanel.load();
            case "tasks"    -> tasksPanel.load();
            case "reports"  -> reportsPanel.load();
            case "projects" -> projectsPanel.load();
            case "profile"  -> profilePanel.load();
        }
    }

    private void handleAdd(String pageId) {
        switch (pageId) {
            case "kanban", "tasks" -> kanbanPanel.showNewTaskDialog(null);
            case "projects"        -> projectsPanel.showNewProjectDialog();
            case "profile"         -> profilePanel.startEdit();
            default -> {}
        }
    }

    void loadProjects() {
        new SwingWorker<List<Project>, Void>() {
            @Override
            protected List<Project> doInBackground() throws Exception {
                Response r = ServerConnection.getInstance().send("GET_PROJECTS", null);
                if (r.isSuccess()) return (List<Project>) r.getData();
                return List.of();
            }
            @Override
            protected void done() {
                try {
                    List<Project> projects = get();
                    sidebar.setProjects(projects);
                    if (!projects.isEmpty() && AppContext.getInstance().getCurrentProject() == null) {
                        AppContext.getInstance().setCurrentProject(projects.get(0));
                    }
                    kanbanPanel.load();
                } catch (Exception e) {
                    // sidebar stays empty
                }
            }
        }.execute();
    }

    /** Loads ~/Downloads/icon.png and sets it as the window icon (silent no-op if missing). */
    static void loadAppIcon(JFrame frame) {
        try {
            File f = new File(System.getProperty("user.home") + "/Downloads/icon.png");
            if (f.exists()) frame.setIconImage(ImageIO.read(f));
        } catch (Exception ignored) {}
    }
}
