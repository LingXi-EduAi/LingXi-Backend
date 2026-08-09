-- BE-08 verification queries. Expected table count: 6.

SELECT COUNT(*) AS ai_table_count
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN (
    'ai_task', 'ai_subtask', 'ai_event',
    'ai_message', 'ai_evidence', 'ai_model_call_log'
  );

SELECT table_name, engine, table_collation
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name LIKE 'ai\_%'
ORDER BY table_name;

SELECT table_name, column_name, data_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name LIKE 'ai\_%'
ORDER BY table_name, ordinal_position;

SELECT table_name, index_name, non_unique,
       GROUP_CONCAT(column_name ORDER BY seq_in_index) AS columns_in_index
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name LIKE 'ai\_%'
GROUP BY table_name, index_name, non_unique
ORDER BY table_name, index_name;
