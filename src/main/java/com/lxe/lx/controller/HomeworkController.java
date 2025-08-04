package com.lxe.lx.controller;

import com.lxe.lx.annotation.Login;
import com.lxe.lx.domain.dto.HomeworkDTO;
import com.lxe.lx.domain.dto.ValidDTO;
import com.lxe.lx.domain.qo.CustomerQO;
import com.lxe.lx.domain.qo.HomeworkQO;
import com.lxe.lx.pojo.Customer;
import com.lxe.lx.pojo.Homework;
import com.lxe.lx.pojo.TokenEntity;
import com.lxe.lx.service.CustomerService;
import com.lxe.lx.service.HomeworkService;
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

import java.util.List;

import static com.lxe.lx.config.AuthorizationInterceptor.ORG_ID_KEY;

@RestController
@RequestMapping("/homework")
public class HomeworkController {
    Logger logger = LogManager.getLogger(HomeworkController.class);

    @Autowired
    private HomeworkService homeworkService;

    @Login
    @RequestMapping(value = "/add", method = RequestMethod.POST)
    public ResultConstant add(HttpServletRequest request, @RequestBody Homework homework) {
        if (homework == null || StringUtils.isBlank(homework.getName())) {
            return ResultConstant.illegalParams("标题不能为空");
        } else if (StringUtils.isBlank(homework.getContent())) {
            return ResultConstant.illegalParams("内容不能为空");
        } else if (StringUtils.isBlank(homework.getFileAddress())) {
            return ResultConstant.illegalParams("文件地址不能为空");
        }
        try {
            TokenEntity tokenEntity = (TokenEntity) request.getAttribute(ORG_ID_KEY);
            homework.setId(Tools.get32UUID());
            homework.setCreateId(tokenEntity.getId());
            homework.setCreateTime(Tools.nowTimeStr());
            homework.setVersion(1);
            ResultConstant ref = homeworkService.add(homework);
            return ref;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("add->error" + e.getMessage());

            return ResultConstant.error("新建账号失败");
        }
    }

    @Login
    @RequestMapping(value = "/edit", method = RequestMethod.POST)
    public ResultConstant edit(HttpServletRequest request, @RequestBody Homework homework) {
//    public ResultConstant edit(HttpServletRequest request,Homework homework) {

        if (homework == null || StringUtils.isBlank(homework.getName())) {
            return ResultConstant.illegalParams("标题不能为空");
        } else if (StringUtils.isBlank(homework.getContent())) {
            return ResultConstant.illegalParams("内容不能为空");
        } else if (StringUtils.isBlank(homework.getFileAddress())) {
            return ResultConstant.illegalParams("文件地址不能为空");
        }
        try {
            TokenEntity tokenEntity = (TokenEntity) request.getAttribute(ORG_ID_KEY);
            Homework temp = homeworkService.getHomeworkById(homework.getId());
            if (temp == null) {
                return ResultConstant.error("当前作业不存在");
            }
            if (!temp.getVersion().equals(homework.getVersion())) {
                return ResultConstant.error("数据已修改，请刷新后重试");
            }
            homework.setUpdateId(tokenEntity.getId());
            homework.setUpdateTime(Tools.nowTimeStr());
            ResultConstant ref = homeworkService.edit(homework);
            return ref;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("add->error" + e.getMessage());
            return ResultConstant.error("编辑作业失败");
        }
    }

    @Login
    @RequestMapping(value = "/detail", method = RequestMethod.POST)
    public ResultConstant detail(HttpServletRequest request, String id) {
        try {
            Homework homework = homeworkService.getHomeworkById(id);
            if (homework == null) {
                return ResultConstant.error("当前作业不存在");
            }
            return ResultConstant.success(homework);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("detail->error" + e.getMessage());

            return ResultConstant.error("查询失败");
        }
    }

    @Login
    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    public ResultConstant delete(HttpServletRequest request, String id) {
        if (StringUtils.isBlank(id)) {
            return ResultConstant.illegalParams("id不能为空");
        }
        try {
            Homework homework = homeworkService.getHomeworkById(id);
            if(homework == null){
                return ResultConstant.error("当前作业不存在");
            }
            ResultConstant ref = homeworkService.delete(id);
            return ref;
        }catch (Exception e) {
            e.printStackTrace();

            logger.error("delete->error"+e.getMessage());
            return ResultConstant.error("删除失败");
        }
    }
    @Login
    @RequestMapping(value = "/list", method = RequestMethod.POST)
    public ResultConstant list(HttpServletRequest request, @RequestBody HomeworkQO homeworkQO) throws Exception {
        ValidDTO validDTO = homeworkQO.validPageParams(homeworkQO);
        if (!validDTO.getResult()) {
            return ResultConstant.illegalParams(validDTO.getMsg());
        }
        try {
            HomeworkDTO homeworkDTO = new HomeworkDTO();
            int count = homeworkService.num(homeworkQO);
            if(count>0){
                List<Homework> list = homeworkService.list(homeworkQO);
                homeworkDTO.setList(list);
            }
            return ResultConstant.success(homeworkDTO);
        }catch (Exception e){
            e.printStackTrace();

            logger.error("list->error"+e.getMessage());
            return ResultConstant.error("查询失败");
        }
    }
}