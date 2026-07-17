package com.lxe.lx.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lxe.lx.annotation.Login;
import com.lxe.lx.domain.dto.DifyChatflowRequest;
import com.lxe.lx.pojo.TokenEntity;
import com.lxe.lx.service.DifyChatflowService;
import com.lxe.lx.util.ResultConstant;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;

import javax.servlet.http.HttpServletRequest;
import java.util.function.Supplier;

import static com.lxe.lx.config.AuthorizationInterceptor.ORG_ID_KEY;

@RestController
@RequestMapping("/api/chatflow")
public class DifyChatflowController {
    private static final Logger logger = LogManager.getLogger(DifyChatflowController.class);

    private final DifyChatflowService chatflowService;
    private final ObjectMapper objectMapper;

    public DifyChatflowController(DifyChatflowService chatflowService, ObjectMapper objectMapper) {
        this.chatflowService = chatflowService;
        this.objectMapper = objectMapper;
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
        } catch (HttpStatusCodeException e) {
            logger.error("Dify Chatflow returned HTTP {}", e.getRawStatusCode());
            return ResultConstant.error(buildDifyErrorMessage(e));
        } catch (ResourceAccessException e) {
            logger.error("Dify Chatflow connection failed: {}", e.getMessage());
            return ResultConstant.error("无法连接 Dify Chatflow 服务");
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.error("Dify Chatflow configuration error: {}", e.getMessage());
            return ResultConstant.error(e.getMessage());
        } catch (Exception e) {
            logger.error("Dify Chatflow invocation failed", e);
            return ResultConstant.error("Chatflow 调用失败");
        }
    }

    private String buildDifyErrorMessage(HttpStatusCodeException exception) {
        String detail = exception.getResponseBodyAsString();
        try {
            JsonNode body = objectMapper.readTree(detail);
            if (body.hasNonNull("message")) {
                detail = body.get("message").asText();
            }
        } catch (Exception ignored) {
            // Keep the original response body when Dify does not return JSON.
        }

        if (StringUtils.isBlank(detail)) {
            detail = HttpStatus.valueOf(exception.getRawStatusCode()).getReasonPhrase();
        }
        return "Dify Chatflow 调用失败：" + StringUtils.abbreviate(detail, 500);
    }
}
