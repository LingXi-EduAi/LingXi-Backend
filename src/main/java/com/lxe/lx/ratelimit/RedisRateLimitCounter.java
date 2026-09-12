package com.lxe.lx.ratelimit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

/**
 * 基于 Redis 的限流计数实现。
 *
 * <p>使用 INCR + EXPIRE 实现固定窗口计数：首次访问设置窗口过期时间，
 * 窗口内每次访问自增，窗口结束后 key 自动过期重新计数。
 *
 * <p>必须使用 {@link StringRedisTemplate}：通用 RedisTemplate 的 JSON 值序列化器
 * 会把脚本参数序列化成带引号的字符串（如 {@code "60"}），导致 {@code EXPIRE} 报
 * {@code ERR value is not an integer or out of range}。
 */
@Component
public class RedisRateLimitCounter implements RateLimitCounter {

    private static final DefaultRedisScript<Long> INCREMENT_SCRIPT = new DefaultRedisScript<>(
            "local count = redis.call('INCR', KEYS[1]); "
                    + "if count == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]); end; "
                    + "return count;",
            Long.class);

    private final StringRedisTemplate redisTemplate;

    public RedisRateLimitCounter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public long incrementAndGet(String key, long windowSeconds) {
        Long count = redisTemplate.execute(
                INCREMENT_SCRIPT,
                java.util.Collections.singletonList(key),
                String.valueOf(windowSeconds));
        return count == null ? 0L : count;
    }
}
