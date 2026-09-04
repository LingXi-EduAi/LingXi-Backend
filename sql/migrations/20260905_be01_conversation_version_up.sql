-- BE-01: align lx_conversation.version with Conversation.version (Integer).
-- Run this migration before deploying code that writes integer versions.
-- Existing non-numeric or out-of-range values are reported and block the ALTER.

DROP PROCEDURE IF EXISTS `lx_be01_validate_conversation_version`;

DELIMITER $$
CREATE PROCEDURE `lx_be01_validate_conversation_version`()
BEGIN
    IF EXISTS (
        SELECT 1
        FROM `lx_conversation`
        WHERE `version` IS NOT NULL
          AND (
              TRIM(`version`) = ''
              OR TRIM(`version`) NOT REGEXP '^[+-]?[0-9]+$'
              OR CAST(TRIM(`version`) AS DECIMAL(65, 0)) < -2147483648
              OR CAST(TRIM(`version`) AS DECIMAL(65, 0)) > 2147483647
          )
    ) THEN
        SELECT `id`, `version` AS `invalid_version`
        FROM `lx_conversation`
        WHERE `version` IS NOT NULL
          AND (
              TRIM(`version`) = ''
              OR TRIM(`version`) NOT REGEXP '^[+-]?[0-9]+$'
              OR CAST(TRIM(`version`) AS DECIMAL(65, 0)) < -2147483648
              OR CAST(TRIM(`version`) AS DECIMAL(65, 0)) > 2147483647
          );
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'BE-01 blocked: lx_conversation.version contains non-integer or out-of-range values';
    END IF;
END$$
DELIMITER ;

CALL `lx_be01_validate_conversation_version`();
DROP PROCEDURE IF EXISTS `lx_be01_validate_conversation_version`;

ALTER TABLE `lx_conversation`
    MODIFY COLUMN `version` INT NULL DEFAULT 1 COMMENT '版本号';
