package com.lxe.lx.ratelimit;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

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

    private RedisTemplate<String, Object> redisTemplate;
    private ValueOperations<String, Object> valueOps;

    private RateLimitService redisService(long firstCount) {
        redisTemplate = mock(RedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment("ratelimit:user-1:/api/ai/tasks")).thenReturn(firstCount);
        return new RateLimitService(new RedisRateLimitCounter(redisTemplate));
    }

    @Test
    void redisFirstCallInWindowReturnsOneAndSetsExpiry() {
        RateLimitService redisService = redisService(1L);

        assertTrue(redisService.tryAcquire("user-1", "/api/ai/tasks", 2, 60));

        verify(redisTemplate).expire("ratelimit:user-1:/api/ai/tasks", 60, TimeUnit.SECONDS);
    }

    @Test
    void redisSecondCallWithinWindowDoesNotResetExpiry() {
        RateLimitService redisService = redisService(2L);

        assertTrue(redisService.tryAcquire("user-1", "/api/ai/tasks", 2, 60));

        verify(redisTemplate, never()).expire(any(), anyLong(), any());
    }

    @Test
    void redisCountOverLimitIsDenied() {
        RateLimitService redisService = redisService(2L);

        assertFalse(redisService.tryAcquire("user-1", "/api/ai/tasks", 1, 60));
    }

    @Test
    void redisKeyUsesRatelimitPrefixWithUserAndPath() {
        redisService(1L).tryAcquire("user-1", "/api/ai/tasks", 2, 60);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOps).increment(keyCaptor.capture());
        assertEquals("ratelimit:user-1:/api/ai/tasks", keyCaptor.getValue());
    }

    @Test
    void redisNullIncrementResultCountsAsZero() {
        redisTemplate = mock(RedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(any())).thenReturn(null);
        RedisRateLimitCounter redisCounter = new RedisRateLimitCounter(redisTemplate);

        assertEquals(0L, redisCounter.incrementAndGet("ratelimit:user-1:/api/ai/tasks", 60));

        verify(redisTemplate, never()).expire(any(), anyLong(), any());
    }
}