package com.lxe.lx.ratelimit;

/**
 * 限流计数存储抽象。
 *
 * <p>将 Redis 计数与测试用内存计数解耦，便于单元测试限流逻辑。
 */
public interface RateLimitCounter {

    /**
     * 对 key 自增并返回当前计数。若 key 不存在则初始化为 1。
     *
     * @param key           计数键（通常为 userId + path + 窗口）
     * @param windowSeconds 窗口秒数，用于设置过期
     * @return 自增后的计数
     */
    long incrementAndGet(String key, long windowSeconds);
}
