-- BE-14: AI access audit log table.
-- Run against the LingXi application database before deploying BE-14 backend code.
CREATE TABLE IF NOT EXISTS `ai_audit_log` (
  `id` varchar(32) NOT NULL COMMENT 'Audit log ID',
  `user_id` varchar(32) NOT NULL COMMENT 'Acting user (customer) ID',
  `path` varchar(255) NOT NULL COMMENT 'Request path',
  `method` varchar(16) NOT NULL COMMENT 'HTTP method',
  `ip` varchar(64) NULL COMMENT 'Client IP',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_ai_audit_user_created` (`user_id`, `created_at`),
  KEY `idx_ai_audit_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI access audit log';
