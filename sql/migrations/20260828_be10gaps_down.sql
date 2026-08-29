-- BE-10 gap fix rollback: drop the columns added by 20260828_be10gaps_up.sql.
-- Only run if the BE-10 gap backend code has been rolled back first.

DROP PROCEDURE IF EXISTS `lx_be10_drop_column_if_exists`;

DELIMITER $$
CREATE PROCEDURE `lx_be10_drop_column_if_exists`()
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'ai_message'
          AND column_name = 'attachments'
    ) THEN
        ALTER TABLE `ai_message` DROP COLUMN `attachments`;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'ai_message'
          AND column_name = 'error_code'
    ) THEN
        ALTER TABLE `ai_message` DROP COLUMN `error_code`;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'ai_message'
          AND column_name = 'error_message'
    ) THEN
        ALTER TABLE `ai_message` DROP COLUMN `error_message`;
    END IF;
END$$
DELIMITER ;

CALL `lx_be10_drop_column_if_exists`();

DROP PROCEDURE IF EXISTS `lx_be10_drop_column_if_exists`;
