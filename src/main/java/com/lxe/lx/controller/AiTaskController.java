package com.lxe.lx.controller;

import com.lxe.lx.annotation.Login;
import com.lxe.lx.domain.dto.AiTaskRequest;
import com.lxe.lx.domain.dto.AiTaskResponse;
import com.lxe.lx.gateway.DifyGatewayException;
import com.lxe.lx.pojo.TokenEntity;
import com.lxe.lx.service.AiTaskService;
import com.lxe.lx.util.ResultConstant;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.lxe.lx.config.AuthorizationInterceptor.ORG_ID_KEY;

@RestController
@RequestMapping("/api/ai")
public class AiTaskController {

    private static final Logger logger = LogManager.getLogger(AiTaskController.class);

    private final AiTaskService aiTaskService;

    public AiTaskController(AiTaskService aiTaskService) {
        this.aiTaskService = aiTaskService;
    }

    @Login
    @PostMapping("/task")
    public ResultConstant sendTask(
            HttpServletRequest request,
            @RequestBody(required = false) AiTaskRequest aiTaskRequest) {
        if (aiTaskRequest == null || StringUtils.isBlank(aiTaskRequest.getQuery())) {
            return ResultConstant.illegalParams("query 不能为空");
        }

        TokenEntity tokenEntity = (TokenEntity) request.getAttribute(ORG_ID_KEY);
        if (tokenEntity == null || StringUtils.isBlank(tokenEntity.getId())) {
            return ResultConstant.error("无法获取当前登录用户");
        }

        try {
            AiTaskResponse response = aiTaskService.sendTask(aiTaskRequest, tokenEntity.getId());
            return ResultConstant.success(response);
        } catch (DifyGatewayException e) {
            logger.error("AI task gateway failed, status={}, retryable={}",
                    e.getHttpStatus(), e.isRetryable());
            return ResultConstant.error(e.getMessage());
        } catch (Exception e) {
            logger.error("AI task invocation failed", e);
            return ResultConstant.error("AI 任务调用失败");
        }
    }

    @Login
    @PostMapping("/task/stream")
    public SseEmitter streamTask(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestBody(required = false) AiTaskRequest aiTaskRequest) {
        if (aiTaskRequest == null || StringUtils.isBlank(aiTaskRequest.getQuery())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "query 不能为空");
        }

        TokenEntity tokenEntity = (TokenEntity) request.getAttribute(ORG_ID_KEY);
        if (tokenEntity == null || StringUtils.isBlank(tokenEntity.getId())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "无法获取当前登录用户");
        }

        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Accel-Buffering", "no");
        return aiTaskService.streamTask(aiTaskRequest, tokenEntity.getId());
    }
}
