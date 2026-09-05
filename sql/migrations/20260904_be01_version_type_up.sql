-- BE-01: 修复 lx_conversation.version 字段类型与 Java 实体不一致。
-- 表定义 (lx_conversation.version) 为 varchar(255)，而实体 Conversation.version 为 Integer。
-- 将存量 varchar 数据安全转换为 int，并修改列类型。
-- 存量值非法（非数字）时按 0 处理，避免转换失败。

ALTER TABLE `lx_conversation`
    MODIFY COLUMN `version` int NULL DEFAULT NULL COMMENT '版本号' AFTER `state`;
