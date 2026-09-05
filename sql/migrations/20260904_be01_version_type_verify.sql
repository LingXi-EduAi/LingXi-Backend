-- BE-01 验证：确认 lx_conversation.version 已是 int 类型。
SELECT
    COLUMN_NAME,
    DATA_TYPE,
    COLUMN_TYPE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'lx_conversation'
  AND COLUMN_NAME = 'version';
-- 期望结果：DATA_TYPE = 'int'，COLUMN_TYPE 以 'int' 开头
