package com.lxe.lx.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lxe.lx.annotation.Login;
import com.lxe.lx.domain.dto.DifyWorkflowRequest;
import com.lxe.lx.pojo.TokenEntity;
import com.lxe.lx.service.DifyWorkflowService;
import com.lxe.lx.util.ResultConstant;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;

import javax.servlet.http.HttpServletRequest;

import static com.lxe.lx.config.AuthorizationInterceptor.ORG_ID_KEY;

@RestController
@RequestMapping("/api/workflow")
public class DifyWorkflowController {
    private static final Logger logger = LogManager.getLogger(DifyWorkflowController.class);

    private final DifyWorkflowService workflowService;
    private final ObjectMapper objectMapper;

    public DifyWorkflowController(DifyWorkflowService workflowService, ObjectMapper objectMapper) {
        this.workflowService = workflowService;
        this.objectMapper = objectMapper;
    }

    @Login
    @PostMapping("/run")
    public ResultConstant runWorkflow(
            HttpServletRequest request,
            @RequestBody(required = false) DifyWorkflowRequest workflowRequest) {
        if (workflowRequest == null) {
            return ResultConstant.illegalParams("请求体不能为空");
        }

        TokenEntity tokenEntity = (TokenEntity) request.getAttribute(ORG_ID_KEY);
        if (tokenEntity == null || StringUtils.isBlank(tokenEntity.getId())) {
            return ResultConstant.error("无法获取当前登录用户");
        }

        try {
            JsonNode response = workflowService.runWorkflow(
                    workflowRequest.getInputs(),
                    tokenEntity.getId()
            );
            return ResultConstant.success(response);
        } catch (HttpStatusCodeException e) {
            logger.error("Dify workflow returned HTTP {}", e.getRawStatusCode());
            return ResultConstant.error(buildDifyErrorMessage(e));
        } catch (ResourceAccessException e) {
            logger.error("Dify workflow connection failed: {}", e.getMessage());
            return ResultConstant.error("无法连接 Dify 工作流服务");
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.error("Dify workflow configuration error: {}", e.getMessage());
            return ResultConstant.error(e.getMessage());
        } catch (Exception e) {
            logger.error("Dify workflow invocation failed", e);
            return ResultConstant.error("工作流调用失败");
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
        return "Dify 工作流调用失败：" + StringUtils.abbreviate(detail, 500);
    }
}
