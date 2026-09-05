package com.lxe.lx.gateway;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI Agent 路由器（BE-05 配置驱动）。
 * <p>
 * 根据 query 中的学科关键词路由到不同的 Dify 应用。
 * 学科 → 应用 的映射通过配置项 ai.agent.route.* 控制
 * （值必须与 {@link DifyChatApplication} 枚举名完全一致，区分大小写），
 * 未匹配到任何学科时默认路由到 CHATFLOW。
 * </p>
 */
@Component
public class AiAgentRouter {

    private static final Logger logger = LogManager.getLogger(AiAgentRouter.class);

    /** 学科标识 → 匹配关键词（英文关键词统一小写，匹配时不区分大小写） */
    private static final Map<String, List<String>> SUBJECT_KEYWORDS;

    static {
        Map<String, List<String>> keywords = new LinkedHashMap<>();
        keywords.put("math", Arrays.asList("数学", "math"));
        keywords.put("physics", Arrays.asList("物理", "physics"));
        SUBJECT_KEYWORDS = Collections.unmodifiableMap(keywords);
    }

    /** 学科标识 → 配置的 Dify 应用（保持配置顺序，先命中先路由） */
    private final Map<String, DifyChatApplication> subjectRoutes;

    public AiAgentRouter(
            @Value("${ai.agent.route.math:CHATFLOW}") String mathRoute,
            @Value("${ai.agent.route.physics:CHATFLOW}") String physicsRoute) {
        subjectRoutes = new LinkedHashMap<>();
        subjectRoutes.put("math", parseApp("math", mathRoute));
        subjectRoutes.put("physics", parseApp("physics", physicsRoute));
    }

    /**
     * 根据用户 query 选择对应的 Dify 应用。
     * <p>
     * 匹配规则：按配置顺序检查各学科关键词，命中即返回该学科配置的应用；
     * query 为空或未命中任何学科时默认返回 CHATFLOW。
     * </p>
     */
    public DifyChatApplication route(String query) {
        if (StringUtils.isBlank(query)) {
            return DifyChatApplication.CHATFLOW;
        }
        String lowerQuery = query.toLowerCase();
        for (Map.Entry<String, DifyChatApplication> entry : subjectRoutes.entrySet()) {
            if (matches(entry.getKey(), lowerQuery)) {
                return entry.getValue();
            }
        }
        return DifyChatApplication.CHATFLOW;
    }

    private boolean matches(String subject, String lowerQuery) {
        List<String> keywords = SUBJECT_KEYWORDS.get(subject);
        if (keywords == null) {
            return false;
        }
        for (String keyword : keywords) {
            if (lowerQuery.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 解析配置值到 {@link DifyChatApplication} 枚举。
     * 配置值必须与枚举名完全一致（区分大小写，如 CHATFLOW）；
     * 为空或非法时记录告警并回退到 CHATFLOW。
     */
    private DifyChatApplication parseApp(String subject, String value) {
        if (StringUtils.isBlank(value)) {
            logger.warn("ai.agent.route.{} 未配置，默认路由到 CHATFLOW", subject);
            return DifyChatApplication.CHATFLOW;
        }
        try {
            return DifyChatApplication.valueOf(value.trim());
        } catch (IllegalArgumentException e) {
            logger.warn("ai.agent.route.{} 配置值 [{}] 无效，默认路由到 CHATFLOW", subject, value);
            return DifyChatApplication.CHATFLOW;
        }
    }
}