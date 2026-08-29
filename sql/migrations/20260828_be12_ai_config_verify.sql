SELECT COUNT(*) AS ai_config_table_count
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name = 'ai_config';

SELECT column_name, data_type, character_maximum_length, is_nullable, column_default
FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'ai_config'
ORDER BY ordinal_position;

SELECT index_name, non_unique,
       GROUP_CONCAT(column_name ORDER BY seq_in_index) AS columns_in_index
FROM information_schema.statistics
WHERE table_schema = DATABASE() AND table_name = 'ai_config'
GROUP BY index_name, non_unique
ORDER BY index_name;
