-- BE-10 gap fix verification: confirm the three columns exist on ai_message.

SELECT column_name, data_type, character_maximum_length, is_nullable, column_default
FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'ai_message'
  AND column_name IN ('attachments', 'error_code', 'error_message')
ORDER BY ordinal_position;
