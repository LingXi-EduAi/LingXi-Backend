package com.lxe.lx.service.impl;

import com.lxe.lx.mapper.AiConfigMapper;
import com.lxe.lx.pojo.AiConfig;
import com.lxe.lx.service.AiConfigService;
import com.lxe.lx.service.AiTaskApiException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AiConfigServiceImpl implements AiConfigService {

    private static final String DEFAULT_ENV = "prod";

    private final AiConfigMapper configMapper;

    public AiConfigServiceImpl(AiConfigMapper configMapper) {
        this.configMapper = configMapper;
    }

    @Override
    public List<AiConfig> listAll(String env) {
        return configMapper.findAll(normalizeEnv(env));
    }

    @Override
    public List<AiConfig> listActive(String env) {
        return configMapper.findAll(normalizeEnv(env)).stream()
                .filter(c -> Boolean.TRUE.equals(c.getActive()))
                .collect(Collectors.toList());
    }

    @Override
    public AiConfig getActive(String configKey, String env) {
        return configMapper.findActive(requireKey(configKey), normalizeEnv(env));
    }

    @Override
    @Transactional
    public AiConfig createVersion(String configKey, String configValue, String env, String remark) {
        String key = requireKey(configKey);
        String value = requireValue(configValue);
        String normalizedEnv = normalizeEnv(env);

        AiConfig config = new AiConfig();
        config.setId(UUID.randomUUID().toString().replace("-", ""));
        config.setConfigKey(key);
        config.setConfigValue(value);
        config.setEnv(normalizedEnv);
        config.setActive(false);
        config.setVersion(nextVersion(key, normalizedEnv));
        config.setRemark(trimToNull(remark));
        config.setCreatedAt(LocalDateTime.now());
        config.setUpdatedAt(config.getCreatedAt());
        configMapper.insert(config);
        return config;
    }

    @Override
    @Transactional
    public AiConfig updateValue(String configKey, String configValue, String env, String remark) {
        String key = requireKey(configKey);
        String value = requireValue(configValue);
        String normalizedEnv = normalizeEnv(env);

        configMapper.deactivateKey(key, normalizedEnv);

        AiConfig config = new AiConfig();
        config.setId(UUID.randomUUID().toString().replace("-", ""));
        config.setConfigKey(key);
        config.setConfigValue(value);
        config.setEnv(normalizedEnv);
        config.setActive(true);
        config.setVersion(nextVersion(key, normalizedEnv));
        config.setRemark(trimToNull(remark));
        config.setCreatedAt(LocalDateTime.now());
        config.setUpdatedAt(config.getCreatedAt());
        configMapper.insert(config);
        return config;
    }

    @Override
    @Transactional
    public AiConfig enableVersion(String id) {
        AiConfig target = configMapper.findById(id);
        if (target == null) {
            throw new AiTaskApiException(404, "配置版本不存在");
        }
        if (!DEFAULT_ENV.equals(target.getEnv())) {
            throw new AiTaskApiException(400, "仅支持操作默认环境(" + DEFAULT_ENV + ")的配置");
        }
        configMapper.deactivateKey(target.getConfigKey(), target.getEnv());
        configMapper.activate(id);
        return configMapper.findActive(target.getConfigKey(), target.getEnv());
    }

    @Override
    @Transactional
    public AiConfig rollback(String configKey, String env, int version) {
        String key = requireKey(configKey);
        String normalizedEnv = normalizeEnv(env);

        AiConfig historical = configMapper.findByKey(key, normalizedEnv).stream()
                .filter(c -> c.getVersion() != null && c.getVersion() == version)
                .findFirst()
                .orElseThrow(() -> new AiTaskApiException(404, "未找到该配置的历史版本"));
        if (Boolean.TRUE.equals(historical.getActive())) {
            throw new AiTaskApiException(400, "该版本已是当前生效版本");
        }
        configMapper.deactivateKey(key, normalizedEnv);
        configMapper.activate(historical.getId());
        return configMapper.findActive(key, normalizedEnv);
    }

    private int nextVersion(String key, String env) {
        Integer max = configMapper.maxVersion(key, env);
        return (max == null ? 0 : max) + 1;
    }

    private String requireKey(String configKey) {
        String key = StringUtils.trimToNull(configKey);
        if (key == null) {
            throw new AiTaskApiException(400, "配置键不能为空");
        }
        return key;
    }

    private String requireValue(String configValue) {
        String value = StringUtils.trimToNull(configValue);
        if (value == null) {
            throw new AiTaskApiException(400, "配置值不能为空");
        }
        return value;
    }

    private String normalizeEnv(String env) {
        return StringUtils.defaultIfBlank(env, DEFAULT_ENV);
    }

    private String trimToNull(String value) {
        return StringUtils.trimToNull(value);
    }
}
