package com.lxe.lx.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.lxe.lx.annotation.Login;
import com.lxe.lx.domain.dto.DifyChatflowRequest;
import com.lxe.lx.gateway.DifyGatewayException;
import com.lxe.lx.pojo.TokenEntity;
import com.lxe.lx.service.DifyChatflowService;
import com.lxe.lx.util.ResultConstant;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.function.Supplier;

import static com.lxe.lx.config.AuthorizationInterceptor.ORG_ID_KEY;

@RestController
@RequestMapping("/api/chatflow")
public class DifyChatflowController {
    private static final Logger logger = LogManager.getLogger(DifyChatflowController.class);

    private final DifyChatflowService chatflowService;

    public DifyChatflowController(DifyChatflowService chatflowService) {
        this.chatflowService = chatflowService;
    }

    @Login
    @PostMapping("/messages")
    public ResultConstant sendMessage(
            HttpServletRequest request,
            @RequestBody(required = false) DifyChatflowRequest chatflowRequest) {
        if (chatflowRequest == null || StringUtils.isBlank(chatflowRequest.getQuery())) {
            return ResultConstant.illegalParams("query 不能为空");
        }

        TokenEntity tokenEntity = (TokenEntity) request.getAttribute(ORG_ID_KEY);
        if (tokenEntity == null || StringUtils.isBlank(tokenEntity.getId())) {
            return ResultConstant.error("无法获取当前登录用户");
        }

        return executeDifyRequest(() -> chatflowService.sendMessage(chatflowRequest, tokenEntity.getId()));
    }

    @Login
    @GetMapping("/messages")
    public ResultConstant getMessages(
            HttpServletRequest request,
            @RequestParam String conversationId,
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(required = false) String firstId) {
        if (StringUtils.isBlank(conversationId)) {
            return ResultConstant.illegalParams("conversationId 不能为空");
        }
        if (limit < 1 || limit > 100) {
            return ResultConstant.illegalParams("limit 必须在 1 到 100 之间");
        }

        TokenEntity tokenEntity = (TokenEntity) request.getAttribute(ORG_ID_KEY);
        if (tokenEntity == null || StringUtils.isBlank(tokenEntity.getId())) {
            return ResultConstant.error("无法获取当前登录用户");
        }

        return executeDifyRequest(() -> chatflowService.getMessages(
                conversationId,
                tokenEntity.getId(),
                limit,
                firstId
        ));
    }

    private ResultConstant executeDifyRequest(Supplier<JsonNode> request) {
        try {
            return ResultConstant.success(request.get());
        } catch (DifyGatewayException e) {
            logger.error("Dify Chatflow gateway failed, status={}, retryable={}",
                    e.getHttpStatus(), e.isRetryable());
            return ResultConstant.error(e.getMessage());
        } catch (Exception e) {
            logger.error("Dify Chatflow invocation failed", e);
            return ResultConstant.error("Chatflow 调用失败");
        }
    }
}
