package com.lxe.lx.gateway;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiAgentRouterTest {

    @Test
    void routesMathQueryToConfiguredApp() {
        AiAgentRouter router = new AiAgentRouter("CHATFLOW", "LEGACY");
        assertEquals(DifyChatApplication.CHATFLOW, router.route("请帮我解一道数学题"));
    }

    @Test
    void routesPhysicsQueryToConfiguredApp() {
        AiAgentRouter router = new AiAgentRouter("CHATFLOW", "LEGACY");
        assertEquals(DifyChatApplication.LEGACY, router.route("这道物理题怎么做"));
    }

    @Test
    void routesEnglishKeywordCaseInsensitively() {
        AiAgentRouter router = new AiAgentRouter("LEGACY", "CHATFLOW");
        assertEquals(DifyChatApplication.LEGACY, router.route("Please solve this Math problem"));
    }

    @Test
    void defaultsToChatflowForUnknownQuery() {
        AiAgentRouter router = new AiAgentRouter("CHATFLOW", "LEGACY");
        assertEquals(DifyChatApplication.CHATFLOW, router.route("帮我写一篇作文"));
    }

    @Test
    void defaultsToChatflowForEmptyQuery() {
        AiAgentRouter router = new AiAgentRouter("CHATFLOW", "LEGACY");
        assertEquals(DifyChatApplication.CHATFLOW, router.route(""));
    }

    @Test
    void defaultsToChatflowForNullQuery() {
        AiAgentRouter router = new AiAgentRouter("CHATFLOW", "LEGACY");
        assertEquals(DifyChatApplication.CHATFLOW, router.route(null));
    }

    @Test
    void fallsBackToChatflowForInvalidConfigValue() {
        AiAgentRouter router = new AiAgentRouter("NOT_A_VALID_APP", "CHATFLOW");
        assertEquals(DifyChatApplication.CHATFLOW, router.route("数学"));
    }

    @Test
    void fallsBackToChatflowForBlankConfigValue() {
        AiAgentRouter router = new AiAgentRouter("", "CHATFLOW");
        assertEquals(DifyChatApplication.CHATFLOW, router.route("数学"));
    }
}