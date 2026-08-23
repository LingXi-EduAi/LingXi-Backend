package com.lxe.lx.controller;

import com.lxe.lx.annotation.Login;
import com.lxe.lx.domain.dto.AiApiResponse;
import com.lxe.lx.domain.dto.AiConversationMessagePage;
import com.lxe.lx.domain.dto.AiConversationPage;
import com.lxe.lx.domain.dto.AiConversationRenameRequest;
import com.lxe.lx.pojo.TokenEntity;
import com.lxe.lx.service.AiConversationService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.UUID;

import static com.lxe.lx.config.AuthorizationInterceptor.ORG_ID_KEY;

@RestController
@RequestMapping("/api/ai/conversations")
public class AiConversationController {
    private final AiConversationService conversationService;

    public AiConversationController(AiConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @Login
    @GetMapping
    public AiApiResponse<AiConversationPage> list(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") int currentPage,
            @RequestParam(defaultValue = "20") int pageSize) {
        return AiApiResponse.success(requestId(),
                conversationService.list(currentUserId(request), currentPage, pageSize));
    }

    @Login
    @GetMapping("/{conversationId}/messages")
    public AiApiResponse<AiConversationMessagePage> messages(
            HttpServletRequest request,
            @PathVariable String conversationId,
            @RequestParam(defaultValue = "1") int currentPage,
            @RequestParam(defaultValue = "20") int pageSize) {
        return AiApiResponse.success(requestId(), conversationService.messages(
                conversationId, currentUserId(request), currentPage, pageSize));
    }

    @Login
    @PatchMapping("/{conversationId}")
    public AiApiResponse<Void> rename(
            HttpServletRequest request,
            @PathVariable String conversationId,
            @RequestBody(required = false) AiConversationRenameRequest renameRequest) {
        if (renameRequest == null || StringUtils.isBlank(renameRequest.getTitle())
                || renameRequest.getTitle().trim().length() > 100) {
            throw new com.lxe.lx.service.AiTaskApiException(400, "标题长度必须为 1 到 100 个字符");
        }
        conversationService.rename(conversationId, currentUserId(request), renameRequest.getTitle());
        return AiApiResponse.success(requestId(), null);
    }

    @Login
    @DeleteMapping("/{conversationId}")
    public AiApiResponse<Void> delete(
            HttpServletRequest request,
            @PathVariable String conversationId) {
        conversationService.delete(conversationId, currentUserId(request));
        return AiApiResponse.success(requestId(), null);
    }

    private String currentUserId(HttpServletRequest request) {
        TokenEntity token = (TokenEntity) request.getAttribute(ORG_ID_KEY);
        if (token == null || StringUtils.isBlank(token.getId())) {
            throw new com.lxe.lx.service.AiTaskApiException(401, "无法获取当前登录用户");
        }
        return token.getId();
    }

    private String requestId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
