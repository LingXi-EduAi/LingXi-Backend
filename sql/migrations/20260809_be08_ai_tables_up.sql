-- BE-08: AI task data model migration (MySQL 8.0+)
-- Run against the LingXi application database before deploying BE-09.

CREATE TABLE `ai_task` (
  `id` varchar(32) NOT NULL COMMENT 'LingXi task ID',
  `user_id` varchar(32) NOT NULL COMMENT 'Owner customer ID',
  `conversation_id` varchar(32) NOT NULL COMMENT 'LingXi conversation ID',
  `task_type` varchar(16) NOT NULL COMMENT 'CHATFLOW or WORKFLOW',
  `status` varchar(32) NOT NULL COMMENT 'Task status',
  `progress` tinyint unsigned NOT NULL DEFAULT 0 COMMENT 'Progress from 0 to 100',
  `request_json` json NOT NULL COMMENT 'Original task request',
  `result_json` json NULL COMMENT 'Final task result',
  `error_code` varchar(64) NULL,
  `error_message` varchar(500) NULL,
  `dify_task_id` varchar(128) NULL COMMENT 'External Dify task ID',
  `dify_conversation_id` varchar(128) NULL COMMENT 'External Dify conversation ID',
  `event_sequence` bigint unsigned NOT NULL DEFAULT 0 COMMENT 'Last allocated event sequence',
  `version` int unsigned NOT NULL DEFAULT 1 COMMENT 'Optimistic lock version',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `started_at` datetime(3) NULL,
  `finished_at` datetime(3) NULL,
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_task_dify` (`task_type`, `dify_task_id`),
  KEY `idx_ai_task_user_status_created` (`user_id`, `status`, `created_at`),
  KEY `idx_ai_task_user_conversation_created` (`user_id`, `conversation_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI task';

CREATE TABLE `ai_subtask` (
  `id` varchar(32) NOT NULL COMMENT 'Stable LingXi subtask ID',
  `task_id` varchar(32) NOT NULL,
  `parent_id` varchar(32) NULL,
  `agent_type` varchar(32) NOT NULL COMMENT 'CHATFLOW or WORKFLOW',
  `goal` text NOT NULL,
  `inputs_json` json NULL,
  `dependency_json` json NULL,
  `status` varchar(32) NOT NULL,
  `execution_no` int unsigned NOT NULL DEFAULT 1,
  `retry_count` int unsigned NOT NULL DEFAULT 0,
  `error_code` varchar(64) NULL,
  `error_message` varchar(500) NULL,
  `version` int unsigned NOT NULL DEFAULT 1,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `started_at` datetime(3) NULL,
  `finished_at` datetime(3) NULL,
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_ai_subtask_task_status` (`task_id`, `status`),
  KEY `idx_ai_subtask_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI subtask';

CREATE TABLE `ai_event` (
  `id` varchar(32) NOT NULL,
  `task_id` varchar(32) NOT NULL,
  `subtask_id` varchar(32) NULL,
  `sequence` bigint unsigned NOT NULL,
  `event_type` varchar(64) NOT NULL,
  `status` varchar(32) NOT NULL,
  `payload_version` int unsigned NOT NULL DEFAULT 1,
  `payload_json` json NOT NULL,
  `source_event_id` varchar(128) NULL COMMENT 'External event identity for idempotency',
  `occurred_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_event_task_sequence` (`task_id`, `sequence`),
  UNIQUE KEY `uk_ai_event_source` (`task_id`, `source_event_id`),
  KEY `idx_ai_event_task_occurred` (`task_id`, `occurred_at`),
  KEY `idx_ai_event_subtask` (`subtask_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI task event';

CREATE TABLE `ai_message` (
  `id` varchar(32) NOT NULL,
  `conversation_id` varchar(32) NOT NULL,
  `task_id` varchar(32) NOT NULL,
  `role` varchar(16) NOT NULL,
  `content` longtext NOT NULL,
  `status` varchar(32) NOT NULL,
  `dify_message_id` varchar(128) NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_message_dify` (`dify_message_id`),
  KEY `idx_ai_message_conversation_created` (`conversation_id`, `created_at`),
  KEY `idx_ai_message_task` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI conversation message';

CREATE TABLE `ai_evidence` (
  `id` varchar(32) NOT NULL,
  `message_id` varchar(32) NOT NULL,
  `source_type` varchar(32) NOT NULL,
  `title` varchar(255) NULL,
  `url` varchar(2048) NULL,
  `content_snippet` text NULL,
  `score` decimal(10,6) NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_ai_evidence_message` (`message_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI answer evidence';

CREATE TABLE `ai_model_call_log` (
  `id` varchar(32) NOT NULL,
  `task_id` varchar(32) NOT NULL,
  `node_name` varchar(255) NULL,
  `model` varchar(128) NULL,
  `total_tokens` bigint unsigned NULL,
  `latency_ms` bigint unsigned NULL,
  `cost` decimal(18,8) NULL,
  `error_code` varchar(64) NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_ai_model_call_task_created` (`task_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI model call audit log';
