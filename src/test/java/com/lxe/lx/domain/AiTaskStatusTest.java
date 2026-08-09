package com.lxe.lx.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiTaskStatusTest {

    @Test
    void onlyAllowsDefinedForwardTransitions() {
        assertTrue(AiTaskStatus.canTransition(AiTaskStatus.CREATED, AiTaskStatus.RUNNING));
        assertTrue(AiTaskStatus.canTransition(AiTaskStatus.RUNNING, AiTaskStatus.SUCCEEDED));
        assertTrue(AiTaskStatus.canTransition(AiTaskStatus.RUNNING, AiTaskStatus.STOPPED));
        assertFalse(AiTaskStatus.canTransition(AiTaskStatus.CREATED, AiTaskStatus.SUCCEEDED));
        assertFalse(AiTaskStatus.canTransition(AiTaskStatus.STOPPED, AiTaskStatus.SUCCEEDED));
    }
}
