-- ═══════════════════════════════════════════════════════════════════════
--  DevTask — complete database schema
--  Run once on a fresh database, or drop/recreate to reset.
--  Tables → Views → Stored Procedures
-- ═══════════════════════════════════════════════════════════════════════

CREATE DATABASE IF NOT EXISTS mongolUpDb
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE mongolUpDb;

-- ── Tables (in dependency order) ─────────────────────────────────────────

CREATE TABLE IF NOT EXISTS users (
    user_id       INT            AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(100)   NOT NULL UNIQUE,
    email         VARCHAR(255)   NOT NULL UNIQUE,
    password_hash VARCHAR(255)   NOT NULL,
    full_name     VARCHAR(255),
    avatar_url    VARCHAR(500),
    role          ENUM('admin','member') DEFAULT 'member',
    is_active     BOOLEAN        DEFAULT TRUE,
    last_login    TIMESTAMP      NULL,
    created_at    TIMESTAMP      DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS sessions (
    session_id INT          AUTO_INCREMENT PRIMARY KEY,
    user_id    INT          NOT NULL,
    token      VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP    NOT NULL,
    created_at TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS projects (
    project_id  INT          AUTO_INCREMENT PRIMARY KEY,
    owner_id    INT          NOT NULL,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    color       VARCHAR(20)  DEFAULT '#378ADD',
    visibility  ENUM('private','public') DEFAULT 'private',
    is_archived BOOLEAN      DEFAULT FALSE,
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES users(user_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS project_members (
    project_id INT  NOT NULL,
    user_id    INT  NOT NULL,
    role       ENUM('manager','member') DEFAULT 'member',
    joined_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (project_id, user_id),
    FOREIGN KEY (project_id) REFERENCES projects(project_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id)    REFERENCES users(user_id)       ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS statuses (
    status_id  INT          AUTO_INCREMENT PRIMARY KEY,
    project_id INT          NOT NULL,
    name       VARCHAR(100) NOT NULL,
    color      VARCHAR(20)  DEFAULT '#9CA3AF',
    type       ENUM('todo','in_progress','review','done') DEFAULT 'todo',
    sort_order INT          DEFAULT 0,
    FOREIGN KEY (project_id) REFERENCES projects(project_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS sprints (
    sprint_id  INT          AUTO_INCREMENT PRIMARY KEY,
    project_id INT          NOT NULL,
    name       VARCHAR(255) NOT NULL,
    start_date DATE         NOT NULL,
    end_date   DATE         NOT NULL,
    is_active  BOOLEAN      DEFAULT FALSE,
    created_at TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES projects(project_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS tasks (
    task_id        INT          AUTO_INCREMENT PRIMARY KEY,
    project_id     INT          NOT NULL,
    parent_task_id INT          NULL,
    created_by     INT,
    status_id      INT          NULL,
    sprint_id      INT          NULL,
    title          VARCHAR(500) NOT NULL,
    description    TEXT,
    weight         INT          DEFAULT 1,
    priority       INT          DEFAULT 2,
    due_date       TIMESTAMP    NULL,
    start_date     TIMESTAMP    NULL,
    completed_at   TIMESTAMP    NULL,
    created_at     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    NULL ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES projects(project_id) ON DELETE CASCADE,
    FOREIGN KEY (status_id)  REFERENCES statuses(status_id)  ON DELETE SET NULL,
    FOREIGN KEY (sprint_id)  REFERENCES sprints(sprint_id)   ON DELETE SET NULL,
    FOREIGN KEY (created_by) REFERENCES users(user_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS task_assignments (
    task_id     INT NOT NULL,
    user_id     INT NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (task_id, user_id),
    FOREIGN KEY (task_id) REFERENCES tasks(task_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS labels (
    label_id   INT          AUTO_INCREMENT PRIMARY KEY,
    project_id INT          NOT NULL,
    name       VARCHAR(100) NOT NULL,
    color      VARCHAR(20)  DEFAULT '#9CA3AF',
    FOREIGN KEY (project_id) REFERENCES projects(project_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS task_labels (
    task_id  INT NOT NULL,
    label_id INT NOT NULL,
    PRIMARY KEY (task_id, label_id),
    FOREIGN KEY (task_id)  REFERENCES tasks(task_id)   ON DELETE CASCADE,
    FOREIGN KEY (label_id) REFERENCES labels(label_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS comments (
    comment_id INT       AUTO_INCREMENT PRIMARY KEY,
    task_id    INT       NOT NULL,
    user_id    INT       NOT NULL,
    content    TEXT      NOT NULL,
    is_edited  BOOLEAN   DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (task_id) REFERENCES tasks(task_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS attachments (
    attachment_id INT          AUTO_INCREMENT PRIMARY KEY,
    task_id       INT          NOT NULL,
    uploaded_by   INT          NOT NULL,
    filename      VARCHAR(255) NOT NULL,
    mime_type     VARCHAR(100) DEFAULT 'application/octet-stream',
    file_size     INT          NOT NULL DEFAULT 0,
    file_data     LONGBLOB     NOT NULL,
    created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (task_id)     REFERENCES tasks(task_id) ON DELETE CASCADE,
    FOREIGN KEY (uploaded_by) REFERENCES users(user_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS activity_logs (
    log_id     INT          AUTO_INCREMENT PRIMARY KEY,
    task_id    INT          NOT NULL,
    user_id    INT,
    action     VARCHAR(100),
    old_value  TEXT,
    new_value  TEXT,
    created_at TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (task_id) REFERENCES tasks(task_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
) ENGINE=InnoDB;

-- ── Views ────────────────────────────────────────────────────────────────

CREATE OR REPLACE VIEW vw_project_overview AS
SELECT
    p.*,
    (SELECT COUNT(*) FROM tasks t WHERE t.project_id = p.project_id)              AS task_count,
    (SELECT COUNT(*) FROM tasks t
     JOIN statuses s ON s.status_id = t.status_id
     WHERE t.project_id = p.project_id AND s.type = 'done')                        AS done_count
FROM projects p;

CREATE OR REPLACE VIEW vw_task_list AS
SELECT
    t.*,
    s.name  AS status_name,
    s.color AS status_color,
    s.type  AS status_type
FROM tasks t
LEFT JOIN statuses s ON s.status_id = t.status_id;

CREATE OR REPLACE VIEW vw_my_tasks AS
SELECT
    t.*,
    s.name    AS status_name,
    s.color   AS status_color,
    s.type    AS status_type,
    ta.user_id AS assignee_id
FROM tasks t
LEFT JOIN statuses s ON s.status_id = t.status_id
JOIN task_assignments ta ON ta.task_id = t.task_id;

CREATE OR REPLACE VIEW vw_overdue_tasks AS
SELECT t.*
FROM tasks t
LEFT JOIN statuses s ON s.status_id = t.status_id
WHERE t.due_date < NOW()
  AND (s.type IS NULL OR s.type != 'done');

CREATE OR REPLACE VIEW vw_active_sprints AS
SELECT * FROM sprints WHERE is_active = TRUE;

-- ── Stored Procedures ─────────────────────────────────────────────────────

DELIMITER $$

-- ┌─ User ──────────────────────────────────────────────────────────────────┐

DROP PROCEDURE IF EXISTS sp_get_user_for_login$$
CREATE PROCEDURE sp_get_user_for_login(IN p_username VARCHAR(100))
BEGIN
    SELECT * FROM users WHERE username = p_username AND is_active = TRUE;
END$$

DROP PROCEDURE IF EXISTS sp_create_session$$
CREATE PROCEDURE sp_create_session(IN p_user_id INT, IN p_token VARCHAR(255))
BEGIN
    INSERT INTO sessions (user_id, token, expires_at)
    VALUES (p_user_id, p_token, DATE_ADD(NOW(), INTERVAL 30 DAY));
END$$

DROP PROCEDURE IF EXISTS sp_update_last_login$$
CREATE PROCEDURE sp_update_last_login(IN p_user_id INT)
BEGIN
    UPDATE users SET last_login = NOW() WHERE user_id = p_user_id;
END$$

DROP PROCEDURE IF EXISTS sp_exists_username$$
CREATE PROCEDURE sp_exists_username(IN p_username VARCHAR(100), OUT p_exists BOOLEAN)
BEGIN
    SELECT EXISTS(SELECT 1 FROM users WHERE username = p_username) INTO p_exists;
END$$

DROP PROCEDURE IF EXISTS sp_exists_email$$
CREATE PROCEDURE sp_exists_email(IN p_email VARCHAR(255), OUT p_exists BOOLEAN)
BEGIN
    SELECT EXISTS(SELECT 1 FROM users WHERE email = p_email) INTO p_exists;
END$$

DROP PROCEDURE IF EXISTS sp_register_user$$
CREATE PROCEDURE sp_register_user(
    IN  p_username      VARCHAR(100),
    IN  p_email         VARCHAR(255),
    IN  p_password_hash VARCHAR(255),
    IN  p_full_name     VARCHAR(255),
    OUT p_user_id       INT
)
BEGIN
    INSERT INTO users (username, email, password_hash, full_name, role, is_active)
    VALUES (p_username, p_email, p_password_hash, p_full_name, 'member', TRUE);
    SET p_user_id = LAST_INSERT_ID();
END$$

DROP PROCEDURE IF EXISTS sp_validate_token$$
CREATE PROCEDURE sp_validate_token(IN p_token VARCHAR(255))
BEGIN
    SELECT u.* FROM users u
    JOIN sessions s ON s.user_id = u.user_id
    WHERE s.token = p_token AND s.expires_at > NOW() AND u.is_active = TRUE;
END$$

DROP PROCEDURE IF EXISTS sp_logout$$
CREATE PROCEDURE sp_logout(IN p_token VARCHAR(255))
BEGIN
    DELETE FROM sessions WHERE token = p_token;
END$$

DROP PROCEDURE IF EXISTS sp_get_all_users$$
CREATE PROCEDURE sp_get_all_users()
BEGIN
    SELECT * FROM users WHERE is_active = TRUE ORDER BY full_name;
END$$

DROP PROCEDURE IF EXISTS sp_get_users_by_project$$
CREATE PROCEDURE sp_get_users_by_project(IN p_project_id INT)
BEGIN
    SELECT u.* FROM users u
    JOIN project_members pm ON pm.user_id = u.user_id
    WHERE pm.project_id = p_project_id;
END$$

DROP PROCEDURE IF EXISTS sp_update_profile$$
CREATE PROCEDURE sp_update_profile(
    IN p_user_id   INT,
    IN p_full_name VARCHAR(255),
    IN p_email     VARCHAR(255)
)
BEGIN
    UPDATE users SET full_name = p_full_name, email = p_email WHERE user_id = p_user_id;
END$$

DROP PROCEDURE IF EXISTS sp_change_password$$
CREATE PROCEDURE sp_change_password(IN p_user_id INT, IN p_new_hash VARCHAR(255))
BEGIN
    UPDATE users SET password_hash = p_new_hash WHERE user_id = p_user_id;
END$$

-- └─ User ──────────────────────────────────────────────────────────────────┘

-- ┌─ Project ───────────────────────────────────────────────────────────────┐

DROP PROCEDURE IF EXISTS sp_get_projects_for_user$$
CREATE PROCEDURE sp_get_projects_for_user(IN p_user_id INT)
BEGIN
    SELECT p.*,
        (SELECT COUNT(*) FROM tasks t WHERE t.project_id = p.project_id)        AS task_count,
        (SELECT COUNT(*) FROM tasks t JOIN statuses s ON s.status_id = t.status_id
         WHERE t.project_id = p.project_id AND s.type = 'done')                 AS done_count
    FROM projects p
    JOIN project_members pm ON pm.project_id = p.project_id
    WHERE pm.user_id = p_user_id AND p.is_archived = FALSE
    ORDER BY p.created_at DESC;
END$$

DROP PROCEDURE IF EXISTS sp_create_project$$
CREATE PROCEDURE sp_create_project(
    IN  p_owner_id    INT,
    IN  p_name        VARCHAR(255),
    IN  p_description TEXT,
    IN  p_color       VARCHAR(20),
    OUT p_project_id  INT
)
BEGIN
    INSERT INTO projects (owner_id, name, description, color, visibility)
    VALUES (p_owner_id, p_name, p_description, p_color, 'private');
    SET p_project_id = LAST_INSERT_ID();
    INSERT IGNORE INTO project_members (project_id, user_id, role)
    VALUES (p_project_id, p_owner_id, 'manager');
    INSERT INTO statuses (project_id, name, color, type, sort_order) VALUES
        (p_project_id, 'Шинэ',           '#9CA3AF', 'todo',        0),
        (p_project_id, 'Хийгдэж байгаа', '#3B82F6', 'in_progress', 1),
        (p_project_id, 'Testing',         '#F59E0B', 'review',      2),
        (p_project_id, 'Дууссан',         '#0F6E56', 'done',        3);
END$$

DROP PROCEDURE IF EXISTS sp_get_project_members$$
CREATE PROCEDURE sp_get_project_members(IN p_project_id INT)
BEGIN
    SELECT u.user_id, u.username, u.full_name, u.avatar_url
    FROM users u
    JOIN project_members pm ON pm.user_id = u.user_id
    WHERE pm.project_id = p_project_id;
END$$

DROP PROCEDURE IF EXISTS sp_add_project_member$$
CREATE PROCEDURE sp_add_project_member(
    IN p_project_id INT,
    IN p_user_id    INT,
    IN p_role       VARCHAR(20)
)
BEGIN
    INSERT IGNORE INTO project_members (project_id, user_id, role)
    VALUES (p_project_id, p_user_id, p_role);
END$$

DROP PROCEDURE IF EXISTS sp_remove_project_member$$
CREATE PROCEDURE sp_remove_project_member(
    IN  p_project_id INT,
    IN  p_user_id    INT,
    OUT p_rows       INT
)
BEGIN
    DELETE FROM project_members
    WHERE project_id = p_project_id AND user_id = p_user_id AND role != 'manager';
    SET p_rows = ROW_COUNT();
END$$

DROP PROCEDURE IF EXISTS sp_archive_project$$
CREATE PROCEDURE sp_archive_project(
    IN  p_project_id INT,
    IN  p_owner_id   INT,
    OUT p_rows       INT
)
BEGIN
    UPDATE projects SET is_archived = TRUE
    WHERE project_id = p_project_id AND owner_id = p_owner_id;
    SET p_rows = ROW_COUNT();
END$$

-- └─ Project ───────────────────────────────────────────────────────────────┘

-- ┌─ Status ────────────────────────────────────────────────────────────────┐

DROP PROCEDURE IF EXISTS sp_get_statuses_by_project$$
CREATE PROCEDURE sp_get_statuses_by_project(IN p_project_id INT)
BEGIN
    SELECT * FROM statuses WHERE project_id = p_project_id ORDER BY sort_order;
END$$

DROP PROCEDURE IF EXISTS sp_create_status$$
CREATE PROCEDURE sp_create_status(
    IN  p_project_id INT,
    IN  p_name       VARCHAR(100),
    IN  p_color      VARCHAR(20),
    OUT p_status_id  INT
)
BEGIN
    INSERT INTO statuses (project_id, name, color, type, sort_order)
    VALUES (p_project_id, p_name, p_color, 'todo',
            (SELECT COALESCE(MAX(s2.sort_order), 0) + 1
             FROM statuses s2 WHERE s2.project_id = p_project_id));
    SET p_status_id = LAST_INSERT_ID();
END$$

DROP PROCEDURE IF EXISTS sp_rename_status$$
CREATE PROCEDURE sp_rename_status(
    IN  p_status_id INT,
    IN  p_new_name  VARCHAR(100),
    OUT p_rows      INT
)
BEGIN
    UPDATE statuses SET name = p_new_name WHERE status_id = p_status_id;
    SET p_rows = ROW_COUNT();
END$$

DROP PROCEDURE IF EXISTS sp_delete_status$$
CREATE PROCEDURE sp_delete_status(
    IN  p_status_id INT,
    OUT p_rows      INT
)
BEGIN
    UPDATE tasks SET status_id = NULL WHERE status_id = p_status_id;
    DELETE FROM statuses WHERE status_id = p_status_id;
    SET p_rows = ROW_COUNT();
END$$

-- └─ Status ────────────────────────────────────────────────────────────────┘

-- ┌─ Sprint ────────────────────────────────────────────────────────────────┐

DROP PROCEDURE IF EXISTS sp_get_active_sprint$$
CREATE PROCEDURE sp_get_active_sprint(IN p_project_id INT)
BEGIN
    SELECT * FROM sprints
    WHERE project_id = p_project_id AND is_active = TRUE
    LIMIT 1;
END$$

-- └─ Sprint ────────────────────────────────────────────────────────────────┘

-- ┌─ Task ──────────────────────────────────────────────────────────────────┐

DROP PROCEDURE IF EXISTS sp_get_tasks_by_project$$
CREATE PROCEDURE sp_get_tasks_by_project(IN p_project_id INT)
BEGIN
    SELECT t.*, s.name AS status_name, s.color AS status_color, s.type AS status_type,
           (SELECT GROUP_CONCAT(CONCAT(u.user_id, ':', COALESCE(u.full_name, u.username))
                    ORDER BY u.full_name SEPARATOR '|')
            FROM task_assignments ta
            JOIN users u ON u.user_id = ta.user_id
            WHERE ta.task_id = t.task_id) AS assignees_csv
    FROM tasks t
    LEFT JOIN statuses s ON s.status_id = t.status_id
    WHERE t.project_id = p_project_id
    ORDER BY t.created_at DESC;
END$$

DROP PROCEDURE IF EXISTS sp_get_my_tasks$$
CREATE PROCEDURE sp_get_my_tasks(IN p_user_id INT)
BEGIN
    SELECT t.*, s.name AS status_name, s.color AS status_color, s.type AS status_type
    FROM tasks t
    LEFT JOIN statuses s ON s.status_id = t.status_id
    JOIN task_assignments ta ON ta.task_id = t.task_id
    WHERE ta.user_id = p_user_id
    ORDER BY t.priority DESC, t.due_date ASC;
END$$

DROP PROCEDURE IF EXISTS sp_get_task_detail$$
CREATE PROCEDURE sp_get_task_detail(IN p_task_id INT)
BEGIN
    SELECT t.*, s.name AS status_name, s.color AS status_color, s.type AS status_type
    FROM tasks t
    LEFT JOIN statuses s ON s.status_id = t.status_id
    WHERE t.task_id = p_task_id;
END$$

DROP PROCEDURE IF EXISTS sp_create_task$$
CREATE PROCEDURE sp_create_task(
    IN  p_project_id INT,
    IN  p_created_by INT,
    IN  p_status_id  INT,
    IN  p_title      VARCHAR(500),
    IN  p_desc       TEXT,
    IN  p_weight     INT,
    IN  p_priority   INT,
    IN  p_due_date   TIMESTAMP,
    IN  p_start_date TIMESTAMP,
    OUT p_task_id    INT
)
BEGIN
    INSERT INTO tasks
        (project_id, created_by, status_id, title, description, weight, priority, due_date, start_date)
    VALUES
        (p_project_id, p_created_by, p_status_id, p_title, p_desc, p_weight, p_priority, p_due_date, p_start_date);
    SET p_task_id = LAST_INSERT_ID();
    INSERT INTO activity_logs (task_id, user_id, action, new_value)
    VALUES (p_task_id, p_created_by, 'task_created', p_title);
END$$

DROP PROCEDURE IF EXISTS sp_update_task$$
CREATE PROCEDURE sp_update_task(
    IN  p_task_id   INT,
    IN  p_title     VARCHAR(500),
    IN  p_desc      TEXT,
    IN  p_weight    INT,
    IN  p_priority  INT,
    IN  p_due_date  TIMESTAMP,
    IN  p_status_id INT,
    OUT p_rows      INT
)
BEGIN
    UPDATE tasks
    SET title = p_title, description = p_desc, weight = p_weight,
        priority = p_priority, due_date = p_due_date, status_id = p_status_id,
        updated_at = NOW()
    WHERE task_id = p_task_id;
    SET p_rows = ROW_COUNT();
END$$

DROP PROCEDURE IF EXISTS sp_update_task_status$$
CREATE PROCEDURE sp_update_task_status(
    IN  p_task_id   INT,
    IN  p_status_id INT,
    IN  p_user_id   INT,
    OUT p_rows      INT
)
BEGIN
    DECLARE v_new_name VARCHAR(100) DEFAULT '';
    UPDATE tasks SET status_id = p_status_id, updated_at = NOW() WHERE task_id = p_task_id;
    SET p_rows = ROW_COUNT();
    IF p_rows > 0 THEN
        SELECT name INTO v_new_name FROM statuses WHERE status_id = p_status_id;
        INSERT INTO activity_logs (task_id, user_id, action, new_value)
        VALUES (p_task_id, p_user_id, 'status_changed', v_new_name);
    END IF;
END$$

DROP PROCEDURE IF EXISTS sp_mark_task_done$$
CREATE PROCEDURE sp_mark_task_done(
    IN  p_task_id    INT,
    IN  p_project_id INT,
    OUT p_rows       INT
)
BEGIN
    UPDATE tasks
    SET status_id = (
        SELECT status_id FROM statuses
        WHERE project_id = p_project_id AND type = 'done'
        ORDER BY sort_order DESC LIMIT 1
    ), updated_at = NOW()
    WHERE task_id = p_task_id;
    SET p_rows = ROW_COUNT();
END$$

DROP PROCEDURE IF EXISTS sp_delete_task$$
CREATE PROCEDURE sp_delete_task(
    IN  p_task_id INT,
    OUT p_rows    INT
)
BEGIN
    DELETE FROM activity_logs    WHERE task_id = p_task_id;
    DELETE FROM tasks            WHERE task_id = p_task_id;
    SET p_rows = ROW_COUNT();
END$$

DROP PROCEDURE IF EXISTS sp_get_assignees$$
CREATE PROCEDURE sp_get_assignees(IN p_task_id INT)
BEGIN
    SELECT u.user_id, u.username, u.full_name, u.avatar_url
    FROM users u
    JOIN task_assignments ta ON ta.user_id = u.user_id
    WHERE ta.task_id = p_task_id;
END$$

DROP PROCEDURE IF EXISTS sp_assign_user$$
CREATE PROCEDURE sp_assign_user(
    IN  p_task_id INT,
    IN  p_user_id INT,
    OUT p_rows    INT
)
BEGIN
    INSERT IGNORE INTO task_assignments (task_id, user_id) VALUES (p_task_id, p_user_id);
    SET p_rows = ROW_COUNT();
END$$

DROP PROCEDURE IF EXISTS sp_remove_assignee$$
CREATE PROCEDURE sp_remove_assignee(IN p_task_id INT, IN p_user_id INT)
BEGIN
    DELETE FROM task_assignments WHERE task_id = p_task_id AND user_id = p_user_id;
END$$

DROP PROCEDURE IF EXISTS sp_get_report_data$$
CREATE PROCEDURE sp_get_report_data(IN p_project_id INT)
BEGIN
    -- Returns sentinel row first (ord=-1), then status counts sorted by sort_order
    SELECT name, cnt, color
    FROM (
        SELECT '__overdue__' AS name,
               COUNT(*)      AS cnt,
               NULL          AS color,
               -1            AS ord
        FROM tasks t
        LEFT JOIN statuses s ON s.status_id = t.status_id
        WHERE t.project_id = p_project_id
          AND t.due_date < NOW()
          AND (s.type IS NULL OR s.type != 'done')

        UNION ALL

        SELECT s.name,
               COUNT(t.task_id) AS cnt,
               s.color,
               s.sort_order     AS ord
        FROM statuses s
        LEFT JOIN tasks t ON t.status_id = s.status_id
        WHERE s.project_id = p_project_id
        GROUP BY s.status_id, s.name, s.color, s.sort_order
    ) combined
    ORDER BY ord;
END$$

-- └─ Task ──────────────────────────────────────────────────────────────────┘

-- ┌─ Label ─────────────────────────────────────────────────────────────────┐

DROP PROCEDURE IF EXISTS sp_get_labels_by_project$$
CREATE PROCEDURE sp_get_labels_by_project(IN p_project_id INT)
BEGIN
    SELECT * FROM labels WHERE project_id = p_project_id ORDER BY name;
END$$

DROP PROCEDURE IF EXISTS sp_get_labels_by_task$$
CREATE PROCEDURE sp_get_labels_by_task(IN p_task_id INT)
BEGIN
    SELECT l.* FROM labels l
    JOIN task_labels tl ON tl.label_id = l.label_id
    WHERE tl.task_id = p_task_id;
END$$

DROP PROCEDURE IF EXISTS sp_add_label_to_task$$
CREATE PROCEDURE sp_add_label_to_task(IN p_task_id INT, IN p_label_id INT)
BEGIN
    INSERT IGNORE INTO task_labels (task_id, label_id) VALUES (p_task_id, p_label_id);
END$$

-- └─ Label ─────────────────────────────────────────────────────────────────┘

-- ┌─ Comment ───────────────────────────────────────────────────────────────┐

DROP PROCEDURE IF EXISTS sp_get_comments_by_task$$
CREATE PROCEDURE sp_get_comments_by_task(IN p_task_id INT)
BEGIN
    SELECT c.*, u.full_name AS author_name, u.username
    FROM comments c
    JOIN users u ON u.user_id = c.user_id
    WHERE c.task_id = p_task_id
    ORDER BY c.created_at ASC;
END$$

DROP PROCEDURE IF EXISTS sp_add_comment$$
CREATE PROCEDURE sp_add_comment(
    IN  p_task_id    INT,
    IN  p_user_id    INT,
    IN  p_content    TEXT,
    OUT p_comment_id INT
)
BEGIN
    INSERT INTO comments (task_id, user_id, content) VALUES (p_task_id, p_user_id, p_content);
    SET p_comment_id = LAST_INSERT_ID();
END$$

DROP PROCEDURE IF EXISTS sp_get_comment_by_id$$
CREATE PROCEDURE sp_get_comment_by_id(IN p_comment_id INT)
BEGIN
    SELECT c.*, u.full_name AS author_name, u.username
    FROM comments c
    JOIN users u ON u.user_id = c.user_id
    WHERE c.comment_id = p_comment_id;
END$$

-- └─ Comment ───────────────────────────────────────────────────────────────┘

-- ┌─ Attachment ────────────────────────────────────────────────────────────┐

DROP PROCEDURE IF EXISTS sp_add_attachment$$
CREATE PROCEDURE sp_add_attachment(
    IN  p_task_id   INT,
    IN  p_user_id   INT,
    IN  p_filename  VARCHAR(255),
    IN  p_mime_type VARCHAR(100),
    IN  p_filesize  INT,
    IN  p_filedata  LONGBLOB,
    OUT p_id        INT
)
BEGIN
    INSERT INTO attachments (task_id, uploaded_by, filename, mime_type, file_size, file_data)
    VALUES (p_task_id, p_user_id, p_filename, p_mime_type, p_filesize, p_filedata);
    SET p_id = LAST_INSERT_ID();
END$$

DROP PROCEDURE IF EXISTS sp_get_attachments_by_task$$
CREATE PROCEDURE sp_get_attachments_by_task(IN p_task_id INT)
BEGIN
    SELECT a.attachment_id, a.task_id, a.uploaded_by, a.filename,
           a.mime_type, a.file_size, a.created_at,
           COALESCE(u.full_name, u.username) AS uploader_name,
           u.username
    FROM attachments a
    JOIN users u ON u.user_id = a.uploaded_by
    WHERE a.task_id = p_task_id
    ORDER BY a.created_at ASC;
END$$

DROP PROCEDURE IF EXISTS sp_get_attachment_file$$
CREATE PROCEDURE sp_get_attachment_file(IN p_id INT)
BEGIN
    SELECT attachment_id, filename, mime_type, file_data
    FROM attachments
    WHERE attachment_id = p_id;
END$$

DROP PROCEDURE IF EXISTS sp_delete_attachment$$
CREATE PROCEDURE sp_delete_attachment(
    IN  p_id      INT,
    IN  p_user_id INT,
    OUT p_rows    INT
)
BEGIN
    DELETE FROM attachments
    WHERE attachment_id = p_id AND uploaded_by = p_user_id;
    SET p_rows = ROW_COUNT();
END$$

-- └─ Attachment ────────────────────────────────────────────────────────────┘

DELIMITER ;
