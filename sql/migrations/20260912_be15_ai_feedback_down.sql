-- BE-15 rollback. Dropping removes generated feedback; regenerate lazily if needed.
DROP TABLE IF EXISTS `ai_feedback`;
