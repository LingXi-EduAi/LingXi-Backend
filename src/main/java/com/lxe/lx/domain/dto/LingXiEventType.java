package com.lxe.lx.domain.dto;

public final class LingXiEventType {
    public static final String TASK_STARTED = "task_started";
    public static final String TASK_DECOMPOSED = "task_decomposed";
    public static final String AGENT_ASSIGNED = "agent_assigned";
    public static final String NODE_PROGRESS = "node_progress";
    public static final String RETRIEVAL_FINISHED = "retrieval_finished";
    public static final String VALIDATION_FINISHED = "validation_finished";
    public static final String ANSWER_DELTA = "answer_delta";
    public static final String TASK_FINISHED = "task_finished";
    public static final String TASK_ERROR = "task_error";
    public static final String EXECUTION_INTERRUPTED = "execution_interrupted";

    private LingXiEventType() {
    }
}
