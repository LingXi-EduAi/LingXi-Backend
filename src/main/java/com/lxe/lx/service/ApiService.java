package com.lxe.lx.service;

import org.springframework.web.multipart.MultipartFile;

public interface ApiService {
    public String sendMessage(String userInput, String userId, String conversationId);

    public String fileUpload(MultipartFile file, String id);

    public String renameConversation(String conversationId, String userId, String newName, boolean autoGenerate);

    public String audioToText(MultipartFile file, String id);

    String getMessages(String conversationId, String userId, int limit, String firstId);

    String getConversations(String userId, String lastId, int limit, String sortBy);

    String deleteConversation(String conversationId, String userId);
}
