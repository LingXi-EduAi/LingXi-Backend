package com.lxe.lx.ratelimit;

import org.springframework.stereotype.Service;

/**
 * 限流服务。
 *
 * <p>按用户 + 路径 + 时间窗口计数，超过阈值返回 {@code false}（应拒绝）。
 * 默认宽松，避免误伤正常用户。
 */
@Service
public class RateLimitService {

    /** 默认窗口内允许的最大请求数（每分钟 60 次/用户）。 */
    public static final int DEFAULT_LIMIT = 60;

    /** 默认窗口秒数（60 秒）。 */
    public static final long DEFAULT_WINDOW_SECONDS = 60L;

    private final RateLimitCounter counter;

    public RateLimitService(RateLimitCounter counter) {
        this.counter = counter;
    }

    /**
     * 尝试获取一次访问许可。
     *
     * @param userId        用户 ID
     * @param path          请求路径
     * @param limit         窗口内允许的最大次数
     * @param windowSeconds 窗口秒数
     * @return {@code true} 放行；{@code false} 超限应拒绝
     */
    public boolean tryAcquire(String userId, String path, int limit, long windowSeconds) {
        if (userId == null || userId.isEmpty() || path == null || path.isEmpty()) {
            return true;
        }
        String key = "ratelimit:" + userId + ":" + path;
        long count = counter.incrementAndGet(key, windowSeconds);
        return count <= limit;
    }
}
