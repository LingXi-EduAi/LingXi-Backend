package com.lxe.lx.service;

import com.lxe.lx.gateway.DifyStream;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class AiRuntimeRegistry {
    private final ConcurrentMap<String, DifyStream> streams = new ConcurrentHashMap<>();

    public void register(String taskId, DifyStream stream) {
        DifyStream previous = streams.put(taskId, stream);
        if (previous != null) {
            previous.cancel();
        }
    }

    public boolean cancel(String taskId) {
        DifyStream stream = streams.remove(taskId);
        if (stream == null) {
            return false;
        }
        stream.cancel();
        return true;
    }

    public void remove(String taskId) {
        streams.remove(taskId);
    }
}
