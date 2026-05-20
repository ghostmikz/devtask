package mongolup.server;

import mongolup.server.dao.*;
import mongolup.server.model.*;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientHandler implements Runnable {
    private static final Logger LOG = Logger.getLogger(ClientHandler.class.getName());

    private final Socket socket;
    private ObjectInputStream  in;
    private ObjectOutputStream out;

    private User currentUser;    // set after successful login/register
    private String currentToken; // session token for this handler

    private final UserDAO       userDAO       = new UserDAO();
    private final ProjectDAO    projectDAO    = new ProjectDAO();
    private final TaskDAO       taskDAO       = new TaskDAO();
    private final StatusDAO     statusDAO     = new StatusDAO();
    private final LabelDAO      labelDAO      = new LabelDAO();
    private final CommentDAO    commentDAO    = new CommentDAO();
    private final SprintDAO     sprintDAO     = new SprintDAO();
    private final AttachmentDAO attachmentDAO = new AttachmentDAO();

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            // OOS must be created before OIS to avoid deadlock
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in  = new ObjectInputStream(socket.getInputStream());

            BroadcastService.getInstance().register(this);

            Request req;
            while ((req = (Request) in.readObject()) != null) {
                handle(req);
            }
        } catch (EOFException | java.net.SocketException e) {
            LOG.info("Client disconnected: " + socket.getRemoteSocketAddress());
        } catch (Exception e) {
            LOG.log(Level.WARNING, "ClientHandler error", e);
        } finally {
            BroadcastService.getInstance().unregister(this);
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    /** Called by BroadcastService to push server-initiated messages. */
    public synchronized void sendPush(Response push) {
        try {
            out.writeObject(push);
            out.flush();
            out.reset(); // prevent memory leak from object cache
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to send push", e);
        }
    }

    // ── request dispatch ─────────────────────────────────────────────────────

    private void handle(Request req) {
        String action = req.getAction();
        Response resp;
        try {
            resp = switch (action) {
                case "LOGIN"              -> handleLogin(req);
                case "REGISTER"          -> handleRegister(req);
                case "LOGOUT"            -> handleLogout(req);
                case "GET_PROJECTS"      -> handleGetProjects(req);
                case "CREATE_PROJECT"    -> handleCreateProject(req);
                case "GET_STATUSES"      -> handleGetStatuses(req);
                case "CREATE_STATUS"     -> handleCreateStatus(req);
                case "RENAME_STATUS"     -> handleRenameStatus(req);
                case "DELETE_STATUS"     -> handleDeleteStatus(req);
                case "GET_TASKS"         -> handleGetTasks(req);
                case "CREATE_TASK"       -> handleCreateTask(req);
                case "UPDATE_TASK"       -> handleUpdateTask(req);
                case "DELETE_TASK"       -> handleDeleteTask(req);
                case "MARK_TASK_DONE"    -> handleMarkTaskDone(req);
                case "UPDATE_TASK_STATUS"-> handleUpdateTaskStatus(req);
                case "GET_TASK_DETAIL"   -> handleGetTaskDetail(req);
                case "GET_MY_TASKS"      -> handleGetMyTasks(req);
                case "GET_USERS"         -> handleGetUsers(req);
                case "GET_ALL_USERS"     -> handleGetAllUsers(req);
                case "ASSIGN_USER"       -> handleAssignUser(req);
                case "REMOVE_ASSIGNEE"   -> handleRemoveAssignee(req);
                case "GET_LABELS"             -> handleGetLabels(req);
                case "ADD_LABEL_TO_TASK"      -> handleAddLabelToTask(req);
                case "REMOVE_LABEL_FROM_TASK" -> handleRemoveLabelFromTask(req);
                case "CREATE_LABEL"           -> handleCreateLabel(req);
                case "ADD_COMMENT"       -> handleAddComment(req);
                case "GET_REPORT_DATA"   -> handleGetReportData(req);
                case "UPDATE_PROFILE"       -> handleUpdateProfile(req);
                case "CHANGE_PASSWORD"      -> handleChangePassword(req);
                case "ADD_PROJECT_MEMBER"   -> handleAddProjectMember(req);
                case "ARCHIVE_PROJECT"      -> handleArchiveProject(req);
                case "REMOVE_PROJECT_MEMBER"-> handleRemoveProjectMember(req);
                case "GET_ACTIVE_SPRINT"    -> handleGetActiveSprint(req);
                case "ADD_ATTACHMENT"       -> handleAddAttachment(req);
                case "GET_ATTACHMENTS"      -> handleGetAttachments(req);
                case "GET_ATTACHMENT_FILE"  -> handleGetAttachmentFile(req);
                case "DELETE_ATTACHMENT"    -> handleDeleteAttachment(req);
                default -> Response.fail(req.getRequestId(), "Unknown action: " + action);
            };
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error handling action " + action, e);
            resp = Response.fail(req.getRequestId(), "Server error: " + e.getMessage());
        }
        send(resp);
    }

    // ── auth ─────────────────────────────────────────────────────────────────

    private Response handleLogin(Request req) throws Exception {
        LoginRequest lr = (LoginRequest) req.getPayload();
        User user = userDAO.login(lr.getUsername(), lr.getPassword());
        if (user == null) return Response.fail(req.getRequestId(), "invalid_credentials");
        currentUser  = user;
        currentToken = user.getSessionToken();
        return Response.ok(req.getRequestId(), user);
    }

    private Response handleRegister(Request req) throws Exception {
        RegisterRequest rr = (RegisterRequest) req.getPayload();
        try {
            User user = userDAO.register(rr.getUsername(), rr.getEmail(),
                    rr.getPassword(), rr.getFullName());
            currentUser  = user;
            currentToken = user.getSessionToken();
            return Response.ok(req.getRequestId(), user);
        } catch (IllegalArgumentException e) {
            return Response.fail(req.getRequestId(), e.getMessage());
        }
    }

    private Response handleLogout(Request req) throws Exception {
        if (req.getToken() != null) userDAO.logout(req.getToken());
        currentUser  = null;
        currentToken = null;
        return Response.ok(req.getRequestId(), null);
    }

    // ── auth guard ────────────────────────────────────────────────────────────

    private User requireAuth(Request req) throws Exception {
        if (currentUser != null) return currentUser;
        User u = userDAO.validateToken(req.getToken());
        if (u == null) throw new SecurityException("unauthorized");
        currentUser  = u;
        currentToken = req.getToken();
        return u;
    }

    // ── projects ─────────────────────────────────────────────────────────────

    private Response handleGetProjects(Request req) throws Exception {
        User u = requireAuth(req);
        List<Project> projects = projectDAO.getProjectsForUser(u.getUserId());
        return Response.ok(req.getRequestId(), (java.io.Serializable) projects);
    }

    private Response handleCreateProject(Request req) throws Exception {
        User u = requireAuth(req);
        Project p = (Project) req.getPayload();
        Project created = projectDAO.createProject(
                u.getUserId(), p.getName(), p.getDescription(), p.getColor());
        BroadcastService.getInstance().broadcast(
                new UpdateEvent(UpdateEvent.Type.PROJECT_CREATED, created.getProjectId(), created), this);
        return Response.ok(req.getRequestId(), created);
    }

    // ── statuses ─────────────────────────────────────────────────────────────

    private Response handleGetStatuses(Request req) throws Exception {
        requireAuth(req);
        int projectId = (Integer) req.getPayload();
        List<Status> statuses = statusDAO.getStatusesByProject(projectId);
        return Response.ok(req.getRequestId(), (java.io.Serializable) statuses);
    }

    private Response handleCreateStatus(Request req) throws Exception {
        requireAuth(req);
        Object[] args = (Object[]) req.getPayload(); // [projectId, name, color]
        Status s = statusDAO.createStatus((Integer) args[0], (String) args[1], (String) args[2]);
        return Response.ok(req.getRequestId(), s);
    }

    private Response handleRenameStatus(Request req) throws Exception {
        requireAuth(req);
        Object[] args = (Object[]) req.getPayload(); // [statusId, newName]
        boolean ok = statusDAO.renameStatus((Integer) args[0], (String) args[1]);
        return ok ? Response.ok(req.getRequestId(), null)
                  : Response.fail(req.getRequestId(), "Status not found");
    }

    private Response handleDeleteStatus(Request req) throws Exception {
        requireAuth(req);
        int statusId = (Integer) req.getPayload();
        boolean ok = statusDAO.deleteStatus(statusId);
        return ok ? Response.ok(req.getRequestId(), null)
                  : Response.fail(req.getRequestId(), "Status not found");
    }

    // ── tasks ─────────────────────────────────────────────────────────────────

    private Response handleGetTasks(Request req) throws Exception {
        requireAuth(req);
        int projectId = (Integer) req.getPayload();
        List<Task> tasks = taskDAO.getTasksByProject(projectId);
        return Response.ok(req.getRequestId(), (java.io.Serializable) tasks);
    }

    private Response handleGetMyTasks(Request req) throws Exception {
        User u = requireAuth(req);
        List<Task> tasks = taskDAO.getMyTasks(u.getUserId());
        return Response.ok(req.getRequestId(), (java.io.Serializable) tasks);
    }

    private Response handleCreateTask(Request req) throws Exception {
        User u = requireAuth(req);
        Task task = (Task) req.getPayload();
        task.setCreatedBy(u.getUserId());
        Task created = taskDAO.createTask(task);
        BroadcastService.getInstance().broadcast(
                new UpdateEvent(UpdateEvent.Type.TASK_CREATED, created.getProjectId(), created), this);
        return Response.ok(req.getRequestId(), created);
    }

    private Response handleUpdateTask(Request req) throws Exception {
        requireAuth(req);
        Task task = (Task) req.getPayload();
        boolean ok = taskDAO.updateTask(task);
        if (ok) BroadcastService.getInstance().broadcast(
                new UpdateEvent(UpdateEvent.Type.TASK_UPDATED, task.getProjectId(), task), this);
        return ok ? Response.ok(req.getRequestId(), task)
                  : Response.fail(req.getRequestId(), "Task not found");
    }

    private Response handleMarkTaskDone(Request req) throws Exception {
        User u = requireAuth(req);
        int[] ids = (int[]) req.getPayload(); // [taskId, projectId]
        boolean ok = taskDAO.markTaskDone(ids[0], ids[1]);
        if (ok) {
            Task stub = new Task();
            stub.setTaskId(ids[0]);
            stub.setProjectId(ids[1]);
            BroadcastService.getInstance().broadcast(
                    new UpdateEvent(UpdateEvent.Type.TASK_STATUS_CHANGED, ids[1], stub), this);
        }
        return ok ? Response.ok(req.getRequestId(), null)
                  : Response.fail(req.getRequestId(), "Update failed");
    }

    private Response handleDeleteTask(Request req) throws Exception {
        requireAuth(req);
        int[] ids = (int[]) req.getPayload(); // [taskId, projectId]
        boolean ok = taskDAO.deleteTask(ids[0]);
        if (ok) {
            Task stub = new Task();
            stub.setTaskId(ids[0]);
            stub.setProjectId(ids[1]);
            BroadcastService.getInstance().broadcast(
                    new UpdateEvent(UpdateEvent.Type.TASK_DELETED, ids[1], stub), this);
        }
        return ok ? Response.ok(req.getRequestId(), null)
                  : Response.fail(req.getRequestId(), "Task not found");
    }

    private Response handleUpdateTaskStatus(Request req) throws Exception {
        User u = requireAuth(req);
        int[] ids = (int[]) req.getPayload(); // [taskId, statusId, projectId]
        boolean ok = taskDAO.updateTaskStatus(ids[0], ids[1], u.getUserId());
        if (ok) {
            Task stub = new Task();
            stub.setTaskId(ids[0]);
            stub.setStatusId(ids[1]);
            stub.setProjectId(ids[2]);
            BroadcastService.getInstance().broadcast(
                    new UpdateEvent(UpdateEvent.Type.TASK_STATUS_CHANGED, ids[2], stub), this);
        }
        return ok ? Response.ok(req.getRequestId(), null)
                  : Response.fail(req.getRequestId(), "Update failed");
    }

    private Response handleGetTaskDetail(Request req) throws Exception {
        requireAuth(req);
        int taskId = (Integer) req.getPayload();
        TaskDetail td = taskDAO.getTaskDetail(taskId);
        return td != null ? Response.ok(req.getRequestId(), td)
                          : Response.fail(req.getRequestId(), "Task not found");
    }

    // ── users & assignments ───────────────────────────────────────────────────

    private Response handleGetUsers(Request req) throws Exception {
        requireAuth(req);
        int projectId = (Integer) req.getPayload();
        List<User> users = userDAO.getUsersByProject(projectId);
        return Response.ok(req.getRequestId(), (java.io.Serializable) users);
    }

    private Response handleGetAllUsers(Request req) throws Exception {
        requireAuth(req);
        List<User> users = userDAO.getAllUsers();
        return Response.ok(req.getRequestId(), (java.io.Serializable) users);
    }

    private Response handleAssignUser(Request req) throws Exception {
        requireAuth(req);
        int[] ids = (int[]) req.getPayload(); // [taskId, userId]
        boolean ok = taskDAO.assignUser(ids[0], ids[1]);
        return ok ? Response.ok(req.getRequestId(), null)
                  : Response.fail(req.getRequestId(), "Already assigned");
    }

    private Response handleRemoveAssignee(Request req) throws Exception {
        requireAuth(req);
        int[] ids = (int[]) req.getPayload(); // [taskId, userId]
        taskDAO.removeAssignee(ids[0], ids[1]);
        return Response.ok(req.getRequestId(), null);
    }

    // ── labels ────────────────────────────────────────────────────────────────

    private Response handleGetLabels(Request req) throws Exception {
        requireAuth(req);
        int projectId = (Integer) req.getPayload();
        List<Label> labels = labelDAO.getLabelsByProject(projectId);
        return Response.ok(req.getRequestId(), (java.io.Serializable) labels);
    }

    private Response handleAddLabelToTask(Request req) throws Exception {
        requireAuth(req);
        int[] ids = (int[]) req.getPayload(); // [taskId, labelId]
        labelDAO.addLabelToTask(ids[0], ids[1]);
        return Response.ok(req.getRequestId(), null);
    }

    private Response handleRemoveLabelFromTask(Request req) throws Exception {
        requireAuth(req);
        int[] ids = (int[]) req.getPayload(); // [taskId, labelId]
        labelDAO.removeLabelFromTask(ids[0], ids[1]);
        return Response.ok(req.getRequestId(), null);
    }

    private Response handleCreateLabel(Request req) throws Exception {
        requireAuth(req);
        Label l = (Label) req.getPayload();
        Label created = labelDAO.createLabel(l.getProjectId(), l.getName(), l.getColor());
        return Response.ok(req.getRequestId(), created);
    }

    // ── comments ─────────────────────────────────────────────────────────────

    private Response handleAddComment(Request req) throws Exception {
        User u = requireAuth(req);
        Comment cm = (Comment) req.getPayload();
        Comment saved = commentDAO.addComment(cm.getTaskId(), u.getUserId(), cm.getContent());
        BroadcastService.getInstance().broadcast(
                new UpdateEvent(UpdateEvent.Type.COMMENT_ADDED, cm.getTaskId(), saved), this);
        return Response.ok(req.getRequestId(), saved);
    }

    // ── reports ───────────────────────────────────────────────────────────────

    private Response handleGetReportData(Request req) throws Exception {
        requireAuth(req);
        int projectId = (Integer) req.getPayload();
        List<Object[]> data = taskDAO.getReportData(projectId);
        return Response.ok(req.getRequestId(), (java.io.Serializable) data);
    }

    // ── profile ───────────────────────────────────────────────────────────────

    private Response handleUpdateProfile(Request req) throws Exception {
        User u = requireAuth(req);
        User updated = (User) req.getPayload();
        userDAO.updateProfile(u.getUserId(), updated.getFullName(), updated.getEmail());
        currentUser.setFullName(updated.getFullName());
        currentUser.setEmail(updated.getEmail());
        return Response.ok(req.getRequestId(), currentUser);
    }

    private Response handleChangePassword(Request req) throws Exception {
        User u = requireAuth(req);
        String[] pw = (String[]) req.getPayload(); // [oldPassword, newPassword]
        boolean ok = userDAO.changePassword(u.getUserId(), pw[0], pw[1]);
        return ok ? Response.ok(req.getRequestId(), null)
                  : Response.fail(req.getRequestId(), "wrong_password");
    }

    // ── project members ───────────────────────────────────────────────────────

    private Response handleArchiveProject(Request req) throws Exception {
        User u = requireAuth(req);
        int projectId = (Integer) req.getPayload();
        boolean ok = projectDAO.archiveProject(projectId, u.getUserId());
        return ok ? Response.ok(req.getRequestId(), null)
                  : Response.fail(req.getRequestId(), "not_owner");
    }

    private Response handleAddProjectMember(Request req) throws Exception {
        requireAuth(req);
        int[] ids = (int[]) req.getPayload(); // [projectId, userId]
        projectDAO.addProjectMember(ids[0], ids[1]);
        return Response.ok(req.getRequestId(), null);
    }

    private Response handleRemoveProjectMember(Request req) throws Exception {
        requireAuth(req);
        int[] ids = (int[]) req.getPayload(); // [projectId, userId]
        boolean ok = projectDAO.removeProjectMember(ids[0], ids[1]);
        return ok ? Response.ok(req.getRequestId(), null)
                  : Response.fail(req.getRequestId(), "cannot_remove");
    }

    // ── sprint ────────────────────────────────────────────────────────────────

    private Response handleGetActiveSprint(Request req) throws Exception {
        requireAuth(req);
        int projectId = (Integer) req.getPayload();
        Sprint sprint = sprintDAO.getActiveSprint(projectId);
        return Response.ok(req.getRequestId(), sprint);
    }

    // ── attachments ───────────────────────────────────────────────────────────

    private Response handleAddAttachment(Request req) throws Exception {
        User u = requireAuth(req);
        Attachment a = (Attachment) req.getPayload();
        Attachment saved = attachmentDAO.addAttachment(
                a.getTaskId(), u.getUserId(), a.getFilename(),
                a.getMimeType(), a.getFileSize(), a.getFileData());
        return Response.ok(req.getRequestId(), saved);
    }

    private Response handleGetAttachments(Request req) throws Exception {
        requireAuth(req);
        int taskId = (Integer) req.getPayload();
        List<Attachment> list = attachmentDAO.getAttachmentsByTask(taskId);
        return Response.ok(req.getRequestId(), (java.io.Serializable) list);
    }

    private Response handleGetAttachmentFile(Request req) throws Exception {
        requireAuth(req);
        int id = (Integer) req.getPayload();
        Attachment a = attachmentDAO.getAttachmentFile(id);
        return a != null ? Response.ok(req.getRequestId(), a)
                         : Response.fail(req.getRequestId(), "not_found");
    }

    private Response handleDeleteAttachment(Request req) throws Exception {
        User u = requireAuth(req);
        int id = (Integer) req.getPayload();
        boolean ok = attachmentDAO.deleteAttachment(id, u.getUserId());
        return ok ? Response.ok(req.getRequestId(), null)
                  : Response.fail(req.getRequestId(), "not_found_or_no_permission");
    }

    // ── I/O helper ────────────────────────────────────────────────────────────

    private synchronized void send(Response resp) {
        try {
            out.writeObject(resp);
            out.flush();
            out.reset();
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to send response", e);
        }
    }
}
