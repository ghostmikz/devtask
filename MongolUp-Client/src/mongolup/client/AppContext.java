package mongolup.client;

import mongolup.server.model.Project;
import mongolup.server.model.User;

/**
 * Singleton that holds global application state for the current session.
 */
public class AppContext {
    private static final AppContext INSTANCE = new AppContext();

    private User currentUser;
    private Project currentProject;

    private AppContext() {}

    public static AppContext getInstance() { return INSTANCE; }

    public User getCurrentUser()            { return currentUser; }
    public void setCurrentUser(User u)      { currentUser = u; }

    public Project getCurrentProject()          { return currentProject; }
    public void setCurrentProject(Project p)    { currentProject = p; }

    public String getToken() {
        return currentUser != null ? currentUser.getSessionToken() : null;
    }

    public void clear() {
        currentUser    = null;
        currentProject = null;
    }
}
