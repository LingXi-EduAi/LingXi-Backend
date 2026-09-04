-- BE-01 verification: the column must be an integer compatible with Java Integer.

SELECT `column_name`, `data_type`, `column_type`, `is_nullable`, `column_default`
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'lx_conversation'
  AND column_name = 'version';

DROP PROCEDURE IF EXISTS `lx_be01_verify_conversation_version`;

DELIMITER $$
CREATE PROCEDURE `lx_be01_verify_conversation_version`()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'lx_conversation'
          AND column_name = 'version'
          AND data_type = 'int'
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'BE-01 verification failed: lx_conversation.version is not INT';
    END IF;
END$$
DELIMITER ;

CALL `lx_be01_verify_conversation_version`();
DROP PROCEDURE IF EXISTS `lx_be01_verify_conversation_version`;
