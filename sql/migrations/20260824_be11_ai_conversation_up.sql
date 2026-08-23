-- BE-11: independent AI conversation metadata.
-- Run before deploying the BE-11 backend code.
CREATE TABLE IF NOT EXISTS `ai_conversation` (
  `id` varchar(32) NOT NULL COMMENT 'LingXi conversation ID',
  `user_id` varchar(32) NOT NULL COMMENT 'Owner customer ID',
  `title` varchar(100) NOT NULL,
  `state` varchar(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE or DELETED',
  `version` int unsigned NOT NULL DEFAULT 1,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `deleted_at` datetime(3) NULL,
  PRIMARY KEY (`id`),
  KEY `idx_ai_conversation_user_state_updated` (`user_id`, `state`, `updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI conversation metadata';

-- Backfill identities already present in BE-09. Existing rows are preserved.
INSERT IGNORE INTO `ai_conversation`
(`id`, `user_id`, `title`, `state`, `version`, `created_at`, `updated_at`)
SELECT t.`conversation_id`, t.`user_id`,
       LEFT(COALESCE(
           JSON_UNQUOTE(JSON_EXTRACT(t.`request_json`, '$.query')),
           'Workflow 会话'
       ), 100),
       'ACTIVE', 1, MIN(t.`created_at`), MAX(t.`updated_at`)
FROM `ai_task` t
GROUP BY t.`conversation_id`, t.`user_id`;
