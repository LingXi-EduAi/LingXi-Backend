-- BE-12 rollback. Do not run after config data is used by production clients
-- unless the data has been backed up and the BE-12 endpoints are disabled.
DROP TABLE IF EXISTS `ai_config`;
