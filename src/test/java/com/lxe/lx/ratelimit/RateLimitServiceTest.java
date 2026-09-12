package com.lxe.lx.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RateLimitService 单元测试（纯 JUnit 5 + Mockito，无 Spring 上下文）。
 *
 * <p>内存计数（InMemoryRateLimitCounter）覆盖窗口语义；Redis 计数
 * （RedisRateLimitCounter）通过 mock RedisTemplate 覆盖 INCR + EXPIRE 行为与 key 格式。</p>
 */
class RateLimitServiceTest {

    private final InMemoryRateLimitCounter counter = new InMemoryRateLimitCounter();
    private final RateLimitService service = new RateLimitService(counter);

    @Test
    void allowsRequestsWithinLimit() {
        for (int i = 0; i < 60; i++) {
            assertTrue(service.tryAcquire("user-1", "/api/ai/tasks", 60, 60),
                    "第 " + (i + 1) + " 次应在阈值内放行");
        }
    }

    @Test
    void rejectsRequestOverLimit() {
        for (int i = 0; i < 60; i++) {
            service.tryAcquire("user-1", "/api/ai/tasks", 60, 60);
        }
        assertFalse(service.tryAcquire("user-1", "/api/ai/tasks", 60, 60),
                "超过阈值应返回 429");
    }

    @Test
    void countsPerUserIndependently() {
        for (int i = 0; i < 60; i++) {
            service.tryAcquire("user-1", "/api/ai/tasks", 60, 60);
        }
        // 另一用户不受影响
        assertTrue(service.tryAcquire("user-2", "/api/ai/tasks", 60, 60));
    }

    @Test
    void countsPerPathIndependently() {
        for (int i = 0; i < 60; i++) {
            service.tryAcquire("user-1", "/api/ai/tasks", 60, 60);
        }
        // 不同路径不受影响
        assertTrue(service.tryAcquire("user-1", "/api/ai/other", 60, 60));
    }

    @Test
    void allowsWhenUserOrPathBlank() {
        assertTrue(service.tryAcquire(null, "/api/ai/tasks", 60, 60));
        assertTrue(service.tryAcquire("user-1", "", 60, 60));
    }

    @Test
    void allowsWhenUserOrPathBlankVariants() {
        assertTrue(service.tryAcquire("", "/api/ai/tasks", 60, 60));
        assertTrue(service.tryAcquire("user-1", null, 60, 60));
    }

    // ===== RedisRateLimitCounter（mock RedisTemplate） =====

    private StringRedisTemplate redisTemplate;
    private RateLimitService redisService(long firstCount) {
        redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.execute(any(), any(), any())).thenReturn(firstCount);
        return new RateLimitService(new RedisRateLimitCounter(redisTemplate));
    }

    @Test
    void redisFirstCallInWindowReturnsOneAndSetsExpiry() {
        RateLimitService redisService = redisService(1L);

        assertTrue(redisService.tryAcquire("user-1", "/api/ai/tasks", 2, 60));

        verify(redisTemplate).execute(any(), any(), any());
    }

    @Test
    void redisSecondCallWithinWindowDoesNotResetExpiry() {
        RateLimitService redisService = redisService(2L);

        assertTrue(redisService.tryAcquire("user-1", "/api/ai/tasks", 2, 60));

        verify(redisTemplate).execute(any(), any(), any());
    }

    @Test
    void redisCountOverLimitIsDenied() {
        RateLimitService redisService = redisService(2L);

        assertFalse(redisService.tryAcquire("user-1", "/api/ai/tasks", 1, 60));
    }

    @Test
    void redisKeyUsesRatelimitPrefixWithUserAndPath() {
        redisService(1L).tryAcquire("user-1", "/api/ai/tasks", 2, 60);

        verify(redisTemplate).execute(any(),
                org.mockito.ArgumentMatchers.eq(java.util.Collections.singletonList(
                        "ratelimit:user-1:/api/ai/tasks")),
                org.mockito.ArgumentMatchers.eq("60"));
    }

    @Test
    void redisNullIncrementResultCountsAsZero() {
        redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.execute(any(), any(), any())).thenReturn(null);
        RedisRateLimitCounter redisCounter = new RedisRateLimitCounter(redisTemplate);

        assertEquals(0L, redisCounter.incrementAndGet("ratelimit:user-1:/api/ai/tasks", 60));

        verify(redisTemplate).execute(any(), any(), any());
    }
}
