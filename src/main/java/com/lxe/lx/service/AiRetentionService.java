package com.lxe.lx.service;

import java.time.LocalDateTime;

/**
 * AI 数据保存期限与删除服务（BE-14）。
 */
public interface AiRetentionService {

    /**
     * 按保留期清理：删除创建时间早于 cutoff 的 AI 相关数据。
     *
     * @param cutoff 截止时间（早于该时间的记录被删除）
     * @return 各表删除行数汇总
     */
    PurgeResult purgeOlderThan(LocalDateTime cutoff);

    /**
     * 手动删除指定会话及其关联数据（消息、证据、任务、事件）。
     *
     * @param userId         所属用户 ID
     * @param conversationId 会话 ID
     * @return 删除行数汇总
     */
    PurgeResult deleteByConversation(String userId, String conversationId);

    /**
     * 手动删除指定任务及其关联数据（事件、模型调用日志、任务本身）。
     *
     * @param userId 所属用户 ID
     * @param taskId 任务 ID
     * @return 删除行数汇总
     */
    PurgeResult deleteByTask(String userId, String taskId);

    /** 各表删除行数汇总。 */
    final class PurgeResult {
        private final int messages;
        private final int evidences;
        private final int events;
        private final int auditLogs;
        private final int modelCallLogs;
        private final int tasks;

        public PurgeResult(int messages, int evidences, int events,
                           int auditLogs, int modelCallLogs, int tasks) {
            this.messages = messages;
            this.evidences = evidences;
            this.events = events;
            this.auditLogs = auditLogs;
            this.modelCallLogs = modelCallLogs;
            this.tasks = tasks;
        }

        public int getMessages() {
            return messages;
        }

        public int getEvidences() {
            return evidences;
        }

        public int getEvents() {
            return events;
        }

        public int getAuditLogs() {
            return auditLogs;
        }

        public int getModelCallLogs() {
            return modelCallLogs;
        }

        public int getTasks() {
            return tasks;
        }

        public int getTotal() {
            return messages + evidences + events + auditLogs + modelCallLogs + tasks;
        }
    }
}
