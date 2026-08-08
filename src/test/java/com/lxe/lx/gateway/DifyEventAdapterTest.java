package com.lxe.lx.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lxe.lx.domain.dto.LingXiEvent;
import com.lxe.lx.domain.dto.LingXiEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class DifyEventAdapterTest {

    private ObjectMapper objectMapper;
    private DifyEventAdapter adapter;
    private DifyEventAdapter.Context context;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        adapter = new DifyEventAdapter(objectMapper);
        context = adapter.createContext("lingxi-task-1", "");
    }

    @Test
    void mapsMessageAndTerminalEventsWithStableSequence() throws Exception {
        LingXiEvent started = adapter.taskStarted(context, "1+1等于多少？");
        LingXiEvent delta = adapter.adapt(context, objectMapper.readTree(
                "{\"event\":\"message\",\"task_id\":\"dify-task-1\","
                        + "\"message_id\":\"message-1\",\"conversation_id\":\"conversation-1\","
                        + "\"answer\":\"2\"}"
        ));
        LingXiEvent finished = adapter.adapt(context, objectMapper.readTree(
                "{\"event\":\"message_end\",\"task_id\":\"dify-task-1\","
                        + "\"message_id\":\"message-1\",\"conversation_id\":\"conversation-1\","
                        + "\"metadata\":{\"usage\":{\"total_tokens\":12}}}"
        ));

        assertEquals(1, started.getSequence());
        assertEquals(LingXiEventType.TASK_STARTED, started.getEventType());
        assertEquals(2, delta.getSequence());
        assertEquals(LingXiEventType.ANSWER_DELTA, delta.getEventType());
        assertEquals("conversation-1", delta.getConversationId());
        assertEquals("2", delta.getPayload().get("delta"));
        assertEquals(3, finished.getSequence());
        assertEquals(LingXiEventType.TASK_FINISHED, finished.getEventType());
        assertEquals("SUCCEEDED", finished.getStatus());
        assertNull(adapter.streamCompleted(context));
    }

    @Test
    void ignoresUnknownDifyEventsWithoutConsumingSequence() throws Exception {
        assertNull(adapter.adapt(context, objectMapper.readTree(
                "{\"event\":\"future_dify_event\",\"data\":{}}"
        )));

        LingXiEvent completed = adapter.streamCompleted(context);
        assertEquals(1, completed.getSequence());
        assertEquals(LingXiEventType.TASK_FINISHED, completed.getEventType());
    }

    @Test
    void mapsDifyErrorAsSingleFailedTerminalEvent() throws Exception {
        LingXiEvent error = adapter.adapt(context, objectMapper.readTree(
                "{\"event\":\"error\",\"status\":503,\"code\":\"upstream_error\","
                        + "\"message\":\"服务暂时不可用\"}"
        ));

        assertEquals(LingXiEventType.TASK_ERROR, error.getEventType());
        assertEquals("FAILED", error.getStatus());
        assertEquals(true, error.getPayload().get("retryable"));
        assertFalse(error.getPayload().containsKey("apiKey"));
        assertNull(adapter.streamCompleted(context));
    }
}
