package com.lxe.lx.gateway;

import org.springframework.stereotype.Component;

/**
 * AI Agent 路由器。
 * <p>
 * 根据 query/subject 路由到不同的 Dify 应用。
 * 默认路由到 CHATFLOW，后续可在 Dify 平台配好学科 Agent 后扩展。
 * </p>
 */
@Component
public class AiAgentRouter {

    /**
     * 根据用户 query 选择对应的 Dify 应用。
     * <p>
     * TODO(BE-02-3): 当 Dify 平台配置好学科 Agent 后，
     * 根据 query 关键词（如 "数学"、"物理"）路由到不同的 DifyChatApplication。
     * </p>
     */
    public DifyChatApplication route(String query) {
        // 目前统一路由到 CHATFLOW，后续扩展
        return DifyChatApplication.CHATFLOW;
    }
}
