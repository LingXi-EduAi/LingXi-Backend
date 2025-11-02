package com.lxe.lx.controller;

import com.lxe.lx.annotation.Login;
import com.lxe.lx.domain.dto.HomeworkAssignmentDTO;
import com.lxe.lx.domain.dto.ValidDTO;
import com.lxe.lx.domain.qo.HomeworkAssignmentQO;
import com.lxe.lx.pojo.HomeworkAssignment;
import com.lxe.lx.pojo.TokenEntity;
import com.lxe.lx.service.HomeworkAssignmentService;
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
@RequestMapping("/homework/assignment")
public class HomeworkAssignmentController {
    Logger logger = LogManager.getLogger(HomeworkAssignmentController.class);

    @Autowired
    private HomeworkAssignmentService homeworkAssignmentService;

    /**
     * 添加作业发布（教师布置作业）
     */
    @Login
    @RequestMapping(value = "/add", method = RequestMethod.POST)
    public ResultConstant add(HttpServletRequest request, @RequestBody HomeworkAssignment homework) {
        if (homework == null || StringUtils.isBlank(homework.getTitle())) {
            return ResultConstant.illegalParams("标题不能为空");
        } else if (StringUtils.isBlank(homework.getContent())) {
            return ResultConstant.illegalParams("内容不能为空");
        } else if (StringUtils.isBlank(homework.getStartTime())) {
            return ResultConstant.illegalParams("开始时间不能为空");
        } else if (StringUtils.isBlank(homework.getEndTime())) {
            return ResultConstant.illegalParams("截止时间不能为空");
        }
        try {
            TokenEntity tokenEntity = (TokenEntity) request.getAttribute(ORG_ID_KEY);
            homework.setId(Tools.get32UUID());
            homework.setTeacherId(tokenEntity.getId());
            homework.setTeacherName(tokenEntity.getName());  // 设置教师姓名
            homework.setCreateId(tokenEntity.getId());
            homework.setCreateTime(Tools.nowTimeStr());
            homework.setState("1");
            homework.setVersion(1);
            
            // 如果没有设置状态，默认为pending
            if (StringUtils.isBlank(homework.getStatus())) {
                homework.setStatus("pending");
            }
            
            ResultConstant ref = homeworkAssignmentService.add(homework);
            return ref;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("add->error" + e.getMessage());
            return ResultConstant.error("布置作业失败");
        }
    }

    /**
     * 编辑作业发布
     */
    @Login
    @RequestMapping(value = "/edit", method = RequestMethod.POST)
    public ResultConstant edit(HttpServletRequest request, @RequestBody HomeworkAssignment homework) {
        if (homework == null || StringUtils.isBlank(homework.getId())) {
            return ResultConstant.illegalParams("作业ID不能为空");
        }
        try {
            TokenEntity tokenEntity = (TokenEntity) request.getAttribute(ORG_ID_KEY);
            HomeworkAssignment temp = homeworkAssignmentService.getById(homework.getId());
            if (temp == null) {
                return ResultConstant.error("当前作业不存在");
            }
            if (!temp.getVersion().equals(homework.getVersion())) {
                return ResultConstant.error("数据已修改，请刷新后重试");
            }
            homework.setUpdateId(tokenEntity.getId());
            homework.setUpdateTime(Tools.nowTimeStr());
            ResultConstant ref = homeworkAssignmentService.edit(homework);
            return ref;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("edit->error" + e.getMessage());
            return ResultConstant.error("编辑作业失败");
        }
    }

    /**
     * 更新作业状态
     */
    @Login
    @RequestMapping(value = "/updateStatus", method = RequestMethod.POST)
    public ResultConstant updateStatus(HttpServletRequest request, @RequestBody HomeworkAssignment homework) {
        if (homework == null || StringUtils.isBlank(homework.getId())) {
            return ResultConstant.illegalParams("作业ID不能为空");
        } else if (StringUtils.isBlank(homework.getStatus())) {
            return ResultConstant.illegalParams("状态不能为空");
        }
        try {
            TokenEntity tokenEntity = (TokenEntity) request.getAttribute(ORG_ID_KEY);
            HomeworkAssignment temp = homeworkAssignmentService.getById(homework.getId());
            if (temp == null) {
                return ResultConstant.error("当前作业不存在");
            }
            if (!temp.getVersion().equals(homework.getVersion())) {
                return ResultConstant.error("数据已修改，请刷新后重试");
            }
            homework.setUpdateId(tokenEntity.getId());
            homework.setUpdateTime(Tools.nowTimeStr());
            ResultConstant ref = homeworkAssignmentService.updateStatus(homework);
            return ref;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("updateStatus->error" + e.getMessage());
            return ResultConstant.error("更新状态失败");
        }
    }

    /**
     * 查询作业详情
     */
    @Login
    @RequestMapping(value = "/detail", method = RequestMethod.POST)
    public ResultConstant detail(HttpServletRequest request, String id) {
        try {
            HomeworkAssignment homework = homeworkAssignmentService.getById(id);
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

    /**
     * 删除作业发布
     */
    @Login
    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    public ResultConstant delete(HttpServletRequest request, String id) {
        if (StringUtils.isBlank(id)) {
            return ResultConstant.illegalParams("id不能为空");
        }
        try {
            HomeworkAssignment homework = homeworkAssignmentService.getById(id);
            if (homework == null) {
                return ResultConstant.error("当前作业不存在");
            }
            ResultConstant ref = homeworkAssignmentService.delete(id);
            return ref;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("delete->error" + e.getMessage());
            return ResultConstant.error("删除失败");
        }
    }

    /**
     * 查询作业列表
     */
    @Login
    @RequestMapping(value = "/list", method = RequestMethod.POST)
    public ResultConstant list(HttpServletRequest request, @RequestBody HomeworkAssignmentQO homeworkQO) throws Exception {
        ValidDTO validDTO = homeworkQO.validPageParams(homeworkQO);
        if (!validDTO.getResult()) {
            return ResultConstant.illegalParams(validDTO.getMsg());
        }
        try {
            HomeworkAssignmentDTO homeworkDTO = new HomeworkAssignmentDTO();
            int count = homeworkAssignmentService.num(homeworkQO);
            homeworkDTO.setCount(count);
            if (count > 0) {
                List<HomeworkAssignment> list = homeworkAssignmentService.list(homeworkQO);
                homeworkDTO.setList(list);
            }
            return ResultConstant.success(homeworkDTO);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("list->error" + e.getMessage());
            return ResultConstant.error("查询失败");
        }
    }
}


