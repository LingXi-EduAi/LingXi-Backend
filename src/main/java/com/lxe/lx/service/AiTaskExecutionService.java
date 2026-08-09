package com.lxe.lx.service;

public interface AiTaskExecutionService {
    void execute(String taskId);

    void retrySubtask(String taskId, String subtaskId);
}
