-- V109__Create_User_Status_History_Table.sql
-- Create audit trail table for user activation/deactivation

CREATE TABLE user_status_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    changed_by_user_id BIGINT NOT NULL,
    previous_status TINYINT(1) NOT NULL COMMENT '0 = inactive, 1 = active',
    new_status TINYINT(1) NOT NULL COMMENT '0 = inactive, 1 = active',
    reason TEXT NOT NULL,
    changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_user_status_history_user
        FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_status_history_changed_by
        FOREIGN KEY (changed_by_user_id) REFERENCES user(id) ON DELETE RESTRICT,

    INDEX idx_user_status_history_user_id (user_id),
    INDEX idx_user_status_history_changed_at (changed_at),
    INDEX idx_user_status_history_changed_by (changed_by_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Audit trail for user activation/deactivation with reasons';

-- Rollback:
-- DROP TABLE user_status_history;
