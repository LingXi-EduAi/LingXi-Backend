-- BE-01 回滚：将 lx_conversation.version 恢复为 varchar(255)。

ALTER TABLE `lx_conversation`
    MODIFY COLUMN `version` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '版本号' AFTER `state`;
