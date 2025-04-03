package com.lxe.lx.service;

import com.lxe.lx.domain.qo.ClassGroupingQO;
import com.lxe.lx.domain.qo.ConversationQO;
import com.lxe.lx.pojo.ClassGrouping;
import com.lxe.lx.pojo.Conversation;
import com.lxe.lx.util.ResultConstant;

import java.util.List;

public interface ConversationService {
    public ResultConstant add(Conversation conversation)throws Exception;
    public ResultConstant edit(Conversation conversation)throws Exception;
    public Conversation getConversationById(String id)throws Exception;
    public int num(ConversationQO conversationQO)throws Exception;
    public List<Conversation> list(ConversationQO conversationQO)throws Exception;
    public ResultConstant deleteById(String id)throws Exception;


}
