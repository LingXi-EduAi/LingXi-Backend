package com.lxe.lx.domain.vo;

import lombok.Data;

/**
 * 学情分析统计响应 VO
 * <p>
 * 对应 /analytics/stats 端点返回的顶层 JSON 结构（status / msg / data）。
 */
@Data
public class AnalyticsStatsResponseVO {

    /** 业务状态码 */
    private Integer status;

    /** 响应消息 */
    private String msg;

    /** 统计数据 */
    private AnalyticsStatsVO data;
}