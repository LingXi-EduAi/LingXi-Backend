-- BE-10 gap fix: persist message attachments and failure info on ai_message.
-- Run against the LingXi application database before deploying the BE-10 gap backend code.
-- Idempotent: each column is added only if it does not already exist.

DROP PROCEDURE IF EXISTS `lx_be10_add_column_if_missing`;

DELIMITER $$
CREATE PROCEDURE `lx_be10_add_column_if_missing`()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'ai_message'
          AND column_name = 'attachments'
    ) THEN
        ALTER TABLE `ai_message`
            ADD COLUMN `attachments` text NULL COMMENT 'Message attachments JSON (FastJSON style)';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'ai_message'
          AND column_name = 'error_code'
    ) THEN
        ALTER TABLE `ai_message`
            ADD COLUMN `error_code` varchar(64) NULL COMMENT 'Failure error code, null on success';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'ai_message'
          AND column_name = 'error_message'
    ) THEN
        ALTER TABLE `ai_message`
            ADD COLUMN `error_message` varchar(500) NULL COMMENT 'Failure error message, null on success';
    END IF;
END$$
DELIMITER ;

CALL `lx_be10_add_column_if_missing`();

DROP PROCEDURE IF EXISTS `lx_be10_add_column_if_missing`;
