package com.lxe.lx.ratelimit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的限流计数实现。
 *
 * <p>使用 INCR + EXPIRE 实现固定窗口计数：首次访问设置窗口过期时间，
 * 窗口内每次访问自增，窗口结束后 key 自动过期重新计数。
 */
@Component
public class RedisRateLimitCounter implements RateLimitCounter {

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisRateLimitCounter(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public long incrementAndGet(String key, long windowSeconds) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
        }
        return count == null ? 0L : count;
    }
}
