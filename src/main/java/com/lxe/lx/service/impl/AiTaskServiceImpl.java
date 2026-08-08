package com.lxe.lx.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.lxe.lx.domain.dto.AiTaskRequest;
import com.lxe.lx.domain.dto.AiTaskResponse;
import com.lxe.lx.domain.dto.DifyChatflowRequest;
import com.lxe.lx.domain.dto.LingXiEvent;
import com.lxe.lx.gateway.AiAgentRouter;
import com.lxe.lx.gateway.DifyEventAdapter;
import com.lxe.lx.gateway.DifyGateway;
import com.lxe.lx.gateway.DifyGatewayException;
import com.lxe.lx.gateway.DifyStream;
import com.lxe.lx.gateway.DifyStreamListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import com.lxe.lx.service.AiTaskService;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@Service
public class AiTaskServiceImpl implements AiTaskService {

    private final DifyGateway difyGateway;
    private final AiAgentRouter agentRouter;
    private final DifyEventAdapter eventAdapter;
    private final TaskScheduler taskScheduler;
    private final long sseTimeoutMs;
    private final long heartbeatMs;

    public AiTaskServiceImpl(
            DifyGateway difyGateway,
            AiAgentRouter agentRouter,
            DifyEventAdapter eventAdapter,
            @Qualifier("aiStreamTaskScheduler") TaskScheduler taskScheduler,
            @Value("${ai.sse.timeout-ms:600000}") long sseTimeoutMs,
            @Value("${ai.sse.heartbeat-ms:15000}") long heartbeatMs) {
        this.difyGateway = difyGateway;
        this.agentRouter = agentRouter;
        this.eventAdapter = eventAdapter;
        this.taskScheduler = taskScheduler;
        this.sseTimeoutMs = sseTimeoutMs;
        this.heartbeatMs = heartbeatMs;
    }

    @Override
    public AiTaskResponse sendTask(AiTaskRequest request, String userId) {
        long startMs = System.currentTimeMillis();

        DifyChatflowRequest difyRequest = new DifyChatflowRequest();
        difyRequest.setQuery(request.getQuery());
        difyRequest.setConversationId(request.getConversationId());

        JsonNode result = difyGateway.sendChatMessage(
                agentRouter.route(request.getQuery()),
                difyRequest,
                userId
        );

        long elapsedMs = System.currentTimeMillis() - startMs;

        AiTaskResponse response = new AiTaskResponse();
        response.setAnswer(result.has("answer") ? result.get("answer").asText() : "");
        response.setConversationId(result.has("conversation_id") ? result.get("conversation_id").asText() : "");
        response.setMessageId(result.has("message_id") ? result.get("message_id").asText() : "");
        response.setElapsedMs(elapsedMs);
        return response;
    }

    @Override
    public SseEmitter streamTask(AiTaskRequest request, String userId) {
        DifyChatflowRequest difyRequest = new DifyChatflowRequest();
        difyRequest.setQuery(request.getQuery());
        difyRequest.setConversationId(request.getConversationId());

        DifyEventAdapter.Context eventContext = eventAdapter.createContext(
                UUID.randomUUID().toString(),
                request.getConversationId()
        );
        AiSseSession session = new AiSseSession(sseTimeoutMs, heartbeatMs, taskScheduler);
        session.send(eventAdapter.taskStarted(eventContext, request.getQuery()));
        session.startHeartbeat();

        try {
            DifyStream stream = difyGateway.streamChatMessage(
                    agentRouter.route(request.getQuery()),
                    difyRequest,
                    userId,
                    new DifyStreamListener() {
                        @Override
                        public void onEvent(JsonNode sourceEvent) {
                            LingXiEvent event = eventAdapter.adapt(eventContext, sourceEvent);
                            if (event != null) {
                                session.send(event);
                            }
                        }

                        @Override
                        public void onComplete() {
                            LingXiEvent event = eventAdapter.streamCompleted(eventContext);
                            if (event != null) {
                                session.send(event);
                            }
                            session.complete();
                        }

                        @Override
                        public void onError(DifyGatewayException exception) {
                            LingXiEvent event = eventAdapter.streamError(
                                    eventContext,
                                    "DIFY_STREAM_ERROR",
                                    exception.getMessage(),
                                    exception.isRetryable()
                            );
                            if (event != null) {
                                session.send(event);
                            }
                            session.complete();
                        }
                    }
            );
            session.attach(stream);
        } catch (DifyGatewayException exception) {
            LingXiEvent event = eventAdapter.streamError(
                    eventContext,
                    "DIFY_STREAM_SETUP_ERROR",
                    exception.getMessage(),
                    exception.isRetryable()
            );
            if (event != null) {
                session.send(event);
            }
            session.complete();
        }
        return session.getEmitter();
    }
}
