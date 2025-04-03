package com.lxe.lx.service;

public interface ChatMessageService {
    public String sendMessage(String userInput, String userId, String conversationId)throws Exception;
}
