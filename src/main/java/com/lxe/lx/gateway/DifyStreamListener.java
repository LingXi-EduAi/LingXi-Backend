package com.lxe.lx.gateway;

import com.fasterxml.jackson.databind.JsonNode;

public interface DifyStreamListener {
    void onEvent(JsonNode event);

    void onComplete();

    void onError(DifyGatewayException exception);
}
