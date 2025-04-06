package com.lxe.lx.service.impl;

import com.lxe.lx.domain.qo.ClassGroupingQO;
import com.lxe.lx.domain.qo.ConversationQO;
import com.lxe.lx.mapper.ClassGroupingMapper;
import com.lxe.lx.mapper.ConversationMapper;
import com.lxe.lx.pojo.ClassGrouping;
import com.lxe.lx.pojo.Conversation;
import com.lxe.lx.service.ConversationService;
import com.lxe.lx.util.ResultConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("ConversationService")
public class ConversationServiceImpl implements ConversationService {
    @Autowired
    private ConversationMapper conversationMapper;
    @Override
    public ResultConstant add(Conversation conversation)throws Exception{
        try{
            conversationMapper.add(conversation);
            return ResultConstant.success("");
        }catch (Exception e){
            e.printStackTrace();
            return ResultConstant.error(e.getMessage());
        }
    }
    @Override
    public ResultConstant edit(Conversation conversation)throws Exception{
        try{
            conversationMapper.edit(conversation);
            return ResultConstant.success("");
        }catch (Exception e){
            e.printStackTrace();
            return ResultConstant.error(e.getMessage());
        }
    }
    @Override
    public Conversation getConversationById(String id)throws Exception{
        return conversationMapper.getConversationById(id);
    }
    @Override
    public Conversation getConversationByConversationId(String conversationId)throws Exception{
        return conversationMapper.getConversationByConversationId(conversationId);

    }

    @Override
    public int num(ConversationQO conversationQO)throws Exception{
        return conversationMapper.num(conversationQO);
    }
    @Override
    public List<Conversation> list(ConversationQO conversationQO)throws Exception{
        return conversationMapper.list(conversationQO);
    }
    @Override
    public ResultConstant deleteById(String id)throws Exception{
        try{
            conversationMapper.deleteById(id);
            return ResultConstant.success("");
        }catch (Exception e){
            e.printStackTrace();
            return ResultConstant.error(e.getMessage());
        }
    }
    @Override
    public ResultConstant deleteByConversationId(String conversationId)throws Exception{
        try{
            conversationMapper.deleteByConversationId(conversationId);
            return ResultConstant.success("");
        }catch (Exception e){
            e.printStackTrace();
            return ResultConstant.error(e.getMessage());
        }
    }

}
