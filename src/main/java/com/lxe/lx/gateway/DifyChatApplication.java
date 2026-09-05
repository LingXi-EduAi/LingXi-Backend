package com.lxe.lx.gateway;

/**
 * Dify 应用键选择器。
 * <p>
 * 用于在 DifyGateway 中区分不同的 Chatflow 应用凭据（旧版 Chatflow 与新版 Chatflow），
 * 从而决定调用哪一个 base-url / api-key。
 * <p>
 * 注意：本类型是普通枚举，<b>不是</b> Spring Boot 启动应用（尽管类名以 Application 结尾）。
 * 它仅作为 Dify 应用键的枚举常量，不包含任何 main 方法或 Spring 启动逻辑。
 * <p>
 * Workflow 调用（{@code /workflows/run}）不经过本枚举：Workflow 使用独立的
 * workflow 凭据（{@code dify.workflow.base-url} / {@code dify.workflow.api-key}），
 * 由 {@code DifyGatewayImpl} 内部直接持有并路由，无需在此枚举中声明 WORKFLOW 常量。
 */
public enum DifyChatApplication {
    /** 旧版 Chatflow 应用 */
    LEGACY,
    /** 新版 Chatflow 应用 */
    CHATFLOW
}
