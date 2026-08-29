-- BE-12: Dify workflow / application version configuration store.
-- Run before deploying the BE-12 backend code.
--
-- Each row is ONE version of a config key (e.g. chatflow.app_id, workflow.prompt_version).
-- A key may have many rows (one per version); at most one row per (env, config_key) is active.
-- "改配置 -> 切换 -> 回滚" is modelled as: insert a new version, then enable it (active=1),
-- or re-enable an older version to roll back.

CREATE TABLE IF NOT EXISTS `ai_config` (
  `id` varchar(32) NOT NULL COMMENT 'Stable config row ID',
  `config_key` varchar(64) NOT NULL COMMENT 'Config key, e.g. chatflow.app_id / workflow.prompt_version',
  `config_value` varchar(512) NOT NULL COMMENT 'Config value',
  `env` varchar(32) NOT NULL DEFAULT 'prod' COMMENT 'Environment identifier, e.g. prod / dev',
  `active` tinyint(1) NOT NULL DEFAULT 0 COMMENT '1 = currently effective version for this key+env',
  `version` int unsigned NOT NULL DEFAULT 1 COMMENT 'Version number, increments per key+env',
  `remark` varchar(255) NULL COMMENT 'Optional human note for this version',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_config_key_env_version` (`config_key`, `env`, `version`),
  KEY `idx_ai_config_key_env_active` (`config_key`, `env`, `active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI Dify application/workflow version configuration';
