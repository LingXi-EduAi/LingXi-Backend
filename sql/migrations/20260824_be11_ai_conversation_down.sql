-- BE-11 rollback. Do not run after conversation data is used by production clients
-- unless the data has been backed up and the BE-11 endpoints are disabled.
DROP TABLE IF EXISTS `ai_conversation`;
