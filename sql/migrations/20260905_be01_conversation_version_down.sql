-- BE-01 rollback: restore the legacy text type.
-- Roll back the application code before running this script.

ALTER TABLE `lx_conversation`
    MODIFY COLUMN `version` VARCHAR(255)
        CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci
        NULL DEFAULT NULL COMMENT '版本号';
