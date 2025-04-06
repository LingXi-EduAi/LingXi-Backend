package com.lxe.lx.mapper;

import com.lxe.lx.domain.qo.ClassGroupingQO;
import com.lxe.lx.domain.qo.ConversationQO;
import com.lxe.lx.pojo.ClassGrouping;
import com.lxe.lx.pojo.Conversation;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper
public interface ConversationMapper {
    void add(Conversation conversation);
    void edit(Conversation conversation);
    Conversation getConversationById(String id);
    Conversation getConversationByConversationId(String conversationId);
    int num(ConversationQO conversationQO);
    List<Conversation> list(ConversationQO conversationQO);
    void deleteById(String id);
    void deleteByConversationId(String conversationId);
}
