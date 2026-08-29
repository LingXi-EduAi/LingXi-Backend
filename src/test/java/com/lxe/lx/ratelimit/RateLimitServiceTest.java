package com.lxe.lx.ratelimit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
