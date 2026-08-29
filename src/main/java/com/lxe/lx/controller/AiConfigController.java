package com.lxe.lx.controller;

import com.lxe.lx.annotation.Login;
import com.lxe.lx.domain.dto.AiApiResponse;
import com.lxe.lx.domain.dto.AiConfigUpdateRequest;
import com.lxe.lx.pojo.AiConfig;
import com.lxe.lx.service.AiConfigService;
import com.lxe.lx.service.AiTaskApiException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ai/configs")
public class AiConfigController {

    private static final Logger logger = LogManager.getLogger(AiConfigController.class);

    private final AiConfigService configService;

    public AiConfigController(AiConfigService configService) {
        this.configService = configService;
    }

    @Login
    @GetMapping
    public AiApiResponse<List<AiConfig>> listAll(
            @RequestParam(value = "env", required = false) String env) {
        return AiApiResponse.success(requestId(), configService.listAll(env));
    }

    @Login
    @GetMapping("/active")
    public AiApiResponse<List<AiConfig>> listActive(
            @RequestParam(value = "env", required = false) String env) {
        return AiApiResponse.success(requestId(), configService.listActive(env));
    }

    @Login
    @PostMapping
    public AiApiResponse<AiConfig> update(
            @RequestBody(required = false) AiConfigUpdateRequest request) {
        if (request == null || StringUtils.isBlank(request.getConfigKey())
                || StringUtils.isBlank(request.getConfigValue())) {
            return AiApiResponse.error(400, "configKey 和 configValue 不能为空", requestId());
        }
        try {
            AiConfig config = configService.updateValue(
                    request.getConfigKey(), request.getConfigValue(),
                    request.getEnv(), request.getRemark());
            return AiApiResponse.success(requestId(), config);
        } catch (AiTaskApiException e) {
            return AiApiResponse.error(e.getHttpStatus(), e.getMessage(), requestId());
        } catch (Exception e) {
            logger.error("Update AI config failed", e);
            return AiApiResponse.error(500, "配置更新失败", requestId());
        }
    }

    @Login
    @PostMapping("/{id}/enable")
    public AiApiResponse<AiConfig> enable(@PathVariable String id) {
        try {
            return AiApiResponse.success(requestId(), configService.enableVersion(id));
        } catch (AiTaskApiException e) {
            return AiApiResponse.error(e.getHttpStatus(), e.getMessage(), requestId());
        } catch (Exception e) {
            logger.error("Enable AI config version failed, id={}", id, e);
            return AiApiResponse.error(500, "启用配置版本失败", requestId());
        }
    }

    private String requestId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
