package com.lxe.lx.controller;

import com.lxe.lx.annotation.Login;
import com.lxe.lx.domain.dto.ClassGroupingDTO;
import com.lxe.lx.domain.dto.ValidDTO;
import com.lxe.lx.domain.qo.ClassGroupingQO;
import com.lxe.lx.pojo.ClassGrouping;
import com.lxe.lx.pojo.Customer;
import com.lxe.lx.pojo.TokenEntity;
import com.lxe.lx.service.ChatMessageService;
import com.lxe.lx.service.ClassGroupingService;
import com.lxe.lx.service.TokenService;
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

import java.util.Arrays;
import java.util.List;

import static com.lxe.lx.config.AuthorizationInterceptor.ORG_ID_KEY;

@RestController
@RequestMapping("/classGrouping")
public class ClassGroupingController {
    Logger logger = LogManager.getLogger(ClassGroupingController.class);
    @Autowired
    private ClassGroupingService classGroupingService;
    @Autowired
    private ChatMessageService chatMessageService;
    @Login
    @RequestMapping(value = "/add", method = RequestMethod.POST)
    public ResultConstant add(HttpServletRequest request, @RequestBody ClassGrouping classGrouping) {
//    public ResultConstant add(HttpServletRequest request, ClassGrouping classGrouping) {
        if (classGrouping == null || StringUtils.isBlank(classGrouping.getClassRule())) {
            return ResultConstant.illegalParams("分班规则不能为空");
        } else if (StringUtils.isBlank(classGrouping.getName())) {
            return ResultConstant.illegalParams("姓名为空");
        } else if (classGrouping.getVolume() == null) {
            return ResultConstant.illegalParams("容量为空");
        } else if (StringUtils.isBlank(classGrouping.getClassCondition())) {
            return ResultConstant.illegalParams("分班条件为空");
        }
        try {
            TokenEntity tokenEntity = (TokenEntity) request.getAttribute(ORG_ID_KEY);
            classGrouping.setId(Tools.get32UUID());
            classGrouping.setCreateId(tokenEntity.getId());
            classGrouping.setCreateTime(Tools.nowTimeStr());
            classGrouping.setState("1");
            classGrouping.setVersion(1);
            ResultConstant ref = classGroupingService.add(classGrouping);
            return ref;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("add->error" + e.getMessage());

            return ResultConstant.error("新建账号失败");
        }
    }

    @Login
    @RequestMapping(value = "/edit", method = RequestMethod.POST)
    public ResultConstant edit(HttpServletRequest request,@RequestBody ClassGrouping classGrouping) {
//    public ResultConstant edit(HttpServletRequest request, ClassGrouping classGrouping) {
        if (classGrouping == null || StringUtils.isBlank(classGrouping.getClassRule())) {
            return ResultConstant.illegalParams("分班规则不能为空");
        } else if (StringUtils.isBlank(classGrouping.getName())) {
            return ResultConstant.illegalParams("姓名为空");
        } else if (classGrouping.getVolume() == null) {
            return ResultConstant.illegalParams("容量为空");
        } else if (StringUtils.isBlank(classGrouping.getClassCondition())) {
            return ResultConstant.illegalParams("分班条件为空");
        } else if (StringUtils.isBlank(classGrouping.getId())) {
            return ResultConstant.illegalParams("id为空");
        } else if (classGrouping.getVersion() == null) {
            return ResultConstant.illegalParams("版本号为空");
        }
        try {
            TokenEntity tokenEntity = (TokenEntity) request.getAttribute(ORG_ID_KEY);
            ClassGrouping temp = classGroupingService.getClassGroupingById(classGrouping.getId());
            if (temp == null) {
                return ResultConstant.error("当前分班模块不存在");
            }
            if (!temp.getVersion().equals(classGrouping.getVersion())) {
                return ResultConstant.error("数据已修改，请刷新后重试");
            }
            classGrouping.setUpdateId(tokenEntity.getId());
            classGrouping.setUpdateTime(Tools.nowTimeStr());
            ResultConstant ref = classGroupingService.edit(classGrouping);
            return ref;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("edit->error" + e.getMessage());
            return ResultConstant.error("修改失败");
        }
    }
    @Login
    @RequestMapping(value = "/list", method = RequestMethod.POST)
    public ResultConstant list(HttpServletRequest request, @RequestBody ClassGroupingQO classGroupingQO) throws Exception {
//    public ResultConstant list(HttpServletRequest request, ClassGroupingQO classGroupingQO) throws Exception {

        ValidDTO validDTO = classGroupingQO.validPageParams(classGroupingQO);
        if (!validDTO.getResult()) {
            return ResultConstant.illegalParams(validDTO.getMsg());
        }
        try {
            ClassGroupingDTO classGroupingDTO = new ClassGroupingDTO();
            int count = classGroupingService.num(classGroupingQO);
            if (count > 0) {
                List<ClassGrouping> list = classGroupingService.list(classGroupingQO);
                classGroupingDTO.setList(list);
            }
            return ResultConstant.success(classGroupingDTO);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("list->error"+e.getMessage());
            return ResultConstant.error("查询失败");
        }
    }
    @Login
    @RequestMapping(value = "/detail", method = RequestMethod.POST)
    public ResultConstant detail(HttpServletRequest request,@RequestBody ClassGrouping classGrouping) {
//    public ResultConstant detail(HttpServletRequest request, ClassGrouping classGrouping) {
        if (StringUtils.isBlank(classGrouping.getId())) {
            ResultConstant.illegalParams("id不能为空");
        }
        try {
            ClassGrouping temp = classGroupingService.getClassGroupingById(classGrouping.getId());
            if(temp!=null){
                return ResultConstant.success(temp);
            }else {
                return ResultConstant.error("当前分班模版不存在");
            }
        } catch (Exception e) {
            e.printStackTrace();

            logger.error("detail->error"+e.getMessage());
            return ResultConstant.error("查看详情失败");
        }

    }
    @Login
    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    public ResultConstant delete(HttpServletResponse response, String id) throws Exception {
        if (StringUtils.isBlank(id)) {
            return ResultConstant.illegalParams("id不能为空");
        }
        try {
            ClassGrouping classGrouping = classGroupingService.getClassGroupingById(id);
            if (classGrouping == null) {
                return ResultConstant.error("当前分班模版不存在");
            }
            ResultConstant ref = classGroupingService.deleteById(id);
            return ref;
        } catch (Exception e) {
            e.printStackTrace();

            logger.error("delete->error"+e.getMessage());
            return ResultConstant.error("删除失败");
        }
    }
}