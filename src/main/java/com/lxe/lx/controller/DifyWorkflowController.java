package com.lxe.lx.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.lxe.lx.annotation.Login;
import com.lxe.lx.domain.dto.DifyWorkflowRequest;
import com.lxe.lx.gateway.DifyGatewayException;
import com.lxe.lx.pojo.TokenEntity;
import com.lxe.lx.service.DifyWorkflowService;
import com.lxe.lx.util.ResultConstant;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

import static com.lxe.lx.config.AuthorizationInterceptor.ORG_ID_KEY;

@RestController
@RequestMapping("/api/workflow")
public class DifyWorkflowController {
    private static final Logger logger = LogManager.getLogger(DifyWorkflowController.class);

    private final DifyWorkflowService workflowService;

    public DifyWorkflowController(DifyWorkflowService workflowService) {
        this.workflowService = workflowService;
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
        } catch (DifyGatewayException e) {
            logger.error("Dify workflow gateway failed, status={}, retryable={}",
                    e.getHttpStatus(), e.isRetryable());
            return ResultConstant.error(e.getMessage());
        } catch (Exception e) {
            logger.error("Dify workflow invocation failed", e);
            return ResultConstant.error("工作流调用失败");
        }
    }
}
