-- BE-08 rollback script.
-- WARNING: do not run after BE-09 starts writing production data.

DROP TABLE IF EXISTS `ai_model_call_log`;
DROP TABLE IF EXISTS `ai_evidence`;
DROP TABLE IF EXISTS `ai_message`;
DROP TABLE IF EXISTS `ai_event`;
DROP TABLE IF EXISTS `ai_subtask`;
DROP TABLE IF EXISTS `ai_task`;
