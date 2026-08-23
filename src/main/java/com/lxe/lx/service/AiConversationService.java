package com.lxe.lx.service;

import com.lxe.lx.domain.dto.AiConversationMessagePage;
import com.lxe.lx.domain.dto.AiConversationPage;
import com.lxe.lx.pojo.AiConversation;

public interface AiConversationService {
    AiConversation createIfAbsent(String conversationId, String userId, String title);

    AiConversation requireActiveOwned(String conversationId, String userId);

    AiConversationPage list(String userId, int page, int size);

    AiConversationMessagePage messages(String conversationId, String userId, int page, int size);

    AiConversation rename(String conversationId, String userId, String title);

    void delete(String conversationId, String userId);
}
