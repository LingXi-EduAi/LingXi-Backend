package com.lxe.lx.controller;

import com.lxe.lx.annotation.Login;
import com.lxe.lx.domain.dto.ConversationDTO;
import com.lxe.lx.domain.dto.ValidDTO;
import com.lxe.lx.domain.qo.ConversationQO;
import com.lxe.lx.pojo.Conversation;
import com.lxe.lx.pojo.Customer;
import com.lxe.lx.pojo.TokenEntity;
import com.lxe.lx.service.ConversationService;
import com.lxe.lx.service.CustomerService;
import com.lxe.lx.util.ResultConstant;
import com.lxe.lx.util.Tools;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.tools.Tool;

import java.util.List;

import static com.lxe.lx.config.AuthorizationInterceptor.ORG_ID_KEY;

@RestController
@RequestMapping("/conversation")
public class ConversationController {
    Logger logger = LogManager.getLogger(ConversationController.class);

    @Autowired
    private ConversationService conversationService;
    @Autowired
    private CustomerService customerService;
    @Login
    @RequestMapping(value = "/add", method = RequestMethod.POST)
    public ResultConstant add(HttpServletRequest request, @RequestBody Conversation conversation) {
        if (conversation == null || StringUtils.isBlank(conversation.getTeacherId())) {
            return ResultConstant.illegalParams("教师id不能为空");
        } else if (StringUtils.isBlank(conversation.getConversationId())) {
            return ResultConstant.illegalParams("会话id不能为空");
        }
        try {
            Customer teacherTemp = customerService.getCustomerById(conversation.getTeacherId());
            if(teacherTemp==null){
                return ResultConstant.error("教师id不存在");
            }
            TokenEntity tokenEntity = (TokenEntity) request.getAttribute(ORG_ID_KEY);
            conversation.setId(Tools.get32UUID());
            conversation.setStudentId(tokenEntity.getId());
            conversation.setCreateId(tokenEntity.getId());
            conversation.setCreateTime(Tools.nowTimeStr());
            conversation.setState("1");
            conversation.setVersion(1);
            ResultConstant ref = conversationService.add(conversation);
            return ref;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("add->error" + e.getMessage());

            return ResultConstant.error("新建会话记录失败");
        }
    }

    @Login
    @RequestMapping(value = "/edit", method = RequestMethod.POST)
    public ResultConstant edit(HttpServletRequest request, @RequestBody Conversation conversation) {
        if (conversation == null || StringUtils.isBlank(conversation.getTeacherId())) {
            return ResultConstant.illegalParams("教师id不能为空");
        } else if (StringUtils.isBlank(conversation.getConversationId())) {
            return ResultConstant.illegalParams("会话id不能为空");
        }
        try{
            TokenEntity tokenEntity = (TokenEntity) request.getAttribute(ORG_ID_KEY);
            conversation.setStudentId(tokenEntity.getId());
            Customer teacherTemp = customerService.getCustomerById(conversation.getTeacherId());
            if(teacherTemp==null){
                return ResultConstant.error("教师id不存在");
            }
            Customer studentTemp = customerService.getCustomerById(conversation.getStudentId());
            if(studentTemp==null){
                return ResultConstant.error("学生id不存在");
            }
            Conversation temp = conversationService.getConversationById(conversation.getId());
            if(temp==null){
                return ResultConstant.error("会话不存在");
            }
            if (!tokenEntity.getId().equals(temp.getStudentId())) {
                return ResultConstant.error("无权修改此会话");
            }
            if (!temp.getVersion().equals(conversation.getVersion())) {
                return ResultConstant.error("数据已修改，请刷新后重试");
            }
            conversation.setUpdateId(tokenEntity.getId());
            conversation.setUpdateTime(Tools.nowTimeStr());
            ResultConstant ref = conversationService.edit(conversation);
            return ref;
        }catch (Exception e) {
            e.printStackTrace();
            logger.error("edit->error" + e.getMessage());
            return ResultConstant.error("修改失败");
        }
    }
    @Login
    @RequestMapping(value = "/list", method = RequestMethod.POST)
    public ResultConstant list(HttpServletRequest request, @RequestBody ConversationQO conversationQO) throws Exception {
        ValidDTO validDTO = conversationQO.validPageParams(conversationQO);
        if (!validDTO.getResult()) {
            return ResultConstant.illegalParams(validDTO.getMsg());
        }
        try {
            TokenEntity tokenEntity = (TokenEntity) request.getAttribute(ORG_ID_KEY);
            conversationQO.setStudentId(tokenEntity.getId());
            ConversationDTO conversationDTO = new ConversationDTO();
            int count = conversationService.num(conversationQO);
            if (count > 0) {
                List<Conversation> list = conversationService.list(conversationQO);
                conversationDTO.setList(list);
            }
            return ResultConstant.success(conversationDTO);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("list->error"+e.getMessage());
            return ResultConstant.error("查询失败");
        }
    }
    @Login
    @RequestMapping(value = "/detail", method = RequestMethod.POST)
    public ResultConstant detail(HttpServletRequest request,@RequestBody Conversation conversation) {
        if (StringUtils.isBlank(conversation.getId())) {
            return ResultConstant.illegalParams("id不能为空");
        }
        try {
            Conversation temp = conversationService.getConversationById(conversation.getId());
            if(temp==null){
                return ResultConstant.error("当前会话记录不存在");
            }
            TokenEntity tokenEntity = (TokenEntity) request.getAttribute(ORG_ID_KEY);
            if (!tokenEntity.getId().equals(temp.getStudentId())) {
                return ResultConstant.error("无权查看此会话");
            }
            return ResultConstant.success(temp);
        } catch (Exception e) {
            e.printStackTrace();

            logger.error("detail->error"+e.getMessage());
            return ResultConstant.error("查看详情失败");
        }

    }
    @Login
    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    public ResultConstant delete(HttpServletRequest request, String id) throws Exception {
        if (StringUtils.isBlank(id)) {
            return ResultConstant.illegalParams("id不能为空");
        }
        try {
            Conversation conversation= conversationService.getConversationById(id);
            if (conversation == null) {
                return ResultConstant.error("当前会话记录不存在");
            }
            TokenEntity tokenEntity = (TokenEntity) request.getAttribute(ORG_ID_KEY);
            if (!tokenEntity.getId().equals(conversation.getStudentId())) {
                return ResultConstant.error("无权删除此会话");
            }
            ResultConstant ref = conversationService.deleteById(id);
            return ref;
        } catch (Exception e) {
            e.printStackTrace();

            logger.error("delete->error"+e.getMessage());
            return ResultConstant.error("删除失败");
        }
    }
}
