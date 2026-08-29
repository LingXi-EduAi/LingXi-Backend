package com.lxe.lx.ratelimit;

import java.util.HashMap;
import java.util.Map;

/**
 * 内存限流计数实现（测试用）。
 *
 * <p>以固定窗口语义模拟 Redis 计数：窗口内计数累加，窗口过期后重置。
 * 仅用于单元测试，不用于生产。
 */
public class InMemoryRateLimitCounter implements RateLimitCounter {

    private final Map<String, Window> windows = new HashMap<>();

    @Override
    public synchronized long incrementAndGet(String key, long windowSeconds) {
        long now = System.currentTimeMillis();
        Window window = windows.get(key);
        if (window == null || now - window.startedAt >= windowSeconds * 1000L) {
            window = new Window(now);
            windows.put(key, window);
        }
        return ++window.count;
    }

    private static final class Window {
        private final long startedAt;
        private long count;

        private Window(long startedAt) {
            this.startedAt = startedAt;
        }
    }
}
