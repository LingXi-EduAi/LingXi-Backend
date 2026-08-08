package com.lxe.lx.domain.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LingXiEventContractTest {

    @Test
    void serializesAllNineV1CommonFields() {
        LingXiEvent event = new LingXiEvent();
        event.setEventId("task-1:1");
        event.setSequence(1);
        event.setEventType(LingXiEventType.TASK_STARTED);
        event.setTaskId("task-1");
        event.setConversationId("");
        event.setOccurredAt("2026-08-09T08:00:00Z");
        event.setStatus("RUNNING");
        event.setPayload(Collections.singletonMap("questionSummary", "1+1等于多少？"));

        JsonNode json = new ObjectMapper().valueToTree(event);

        assertEquals(9, json.size());
        assertEquals("task-1:1", json.path("eventId").asText());
        assertEquals(1, json.path("sequence").asLong());
        assertEquals(1, json.path("payloadVersion").asInt());
        assertEquals("1+1等于多少？", json.path("payload").path("questionSummary").asText());
    }

    @Test
    void exposesTheNineFrozenV1EventTypes() {
        Set<String> eventTypes = new HashSet<>();
        eventTypes.add(LingXiEventType.TASK_STARTED);
        eventTypes.add(LingXiEventType.TASK_DECOMPOSED);
        eventTypes.add(LingXiEventType.AGENT_ASSIGNED);
        eventTypes.add(LingXiEventType.NODE_PROGRESS);
        eventTypes.add(LingXiEventType.RETRIEVAL_FINISHED);
        eventTypes.add(LingXiEventType.VALIDATION_FINISHED);
        eventTypes.add(LingXiEventType.ANSWER_DELTA);
        eventTypes.add(LingXiEventType.TASK_FINISHED);
        eventTypes.add(LingXiEventType.TASK_ERROR);

        assertEquals(9, eventTypes.size());
    }
}
