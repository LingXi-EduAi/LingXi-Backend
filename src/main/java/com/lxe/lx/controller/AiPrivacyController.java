package com.lxe.lx.controller;

import com.lxe.lx.annotation.Login;
import com.lxe.lx.annotation.TeacherOnly;
import com.lxe.lx.domain.dto.AiApiResponse;
import com.lxe.lx.service.AiRetentionService;
import com.lxe.lx.service.AiRetentionService.PurgeResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * AI 数据隐私与保留管理接口（BE-14）。
 *
 * <p>提供按保留期清理与手动按会话/任务删除能力。仅教师可执行清理操作。
 */
@RestController
@RequestMapping("/api/ai/privacy")
public class AiPrivacyController {

    private final AiRetentionService retentionService;
    private final int retentionDays;

    public AiPrivacyController(AiRetentionService retentionService,
                               @Value("${ai.privacy.retention-days:90}") int retentionDays) {
        this.retentionService = retentionService;
        this.retentionDays = retentionDays;
    }

    @Login
    @TeacherOnly
    @PostMapping("/purge")
    public AiApiResponse<PurgeResult> purgeExpired() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        return AiApiResponse.success(requestId(), retentionService.purgeOlderThan(cutoff));
    }

    @Login
    @DeleteMapping("/conversations/{conversationId}")
    public AiApiResponse<PurgeResult> deleteConversation(
            HttpServletRequest request,
            @PathVariable String conversationId) {
        String userId = currentUserId(request);
        return AiApiResponse.success(requestId(),
                retentionService.deleteByConversation(userId, conversationId));
    }

    @Login
    @DeleteMapping("/tasks/{taskId}")
    public AiApiResponse<PurgeResult> deleteTask(
            HttpServletRequest request,
            @PathVariable String taskId) {
        String userId = currentUserId(request);
        return AiApiResponse.success(requestId(),
                retentionService.deleteByTask(userId, taskId));
    }

    private String currentUserId(HttpServletRequest request) {
        com.lxe.lx.pojo.TokenEntity token =
                (com.lxe.lx.pojo.TokenEntity) request.getAttribute(
                        com.lxe.lx.config.AuthorizationInterceptor.ORG_ID_KEY);
        return token == null ? null : token.getId();
    }

    private String requestId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
