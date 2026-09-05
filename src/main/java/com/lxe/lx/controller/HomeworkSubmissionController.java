package com.lxe.lx.controller;

import com.lxe.lx.annotation.Login;
import com.lxe.lx.annotation.TeacherOnly;
import com.lxe.lx.domain.dto.HomeworkSubmissionDTO;
import com.lxe.lx.domain.dto.ValidDTO;
import com.lxe.lx.domain.qo.HomeworkSubmissionQO;
import com.lxe.lx.pojo.HomeworkSubmission;
import com.lxe.lx.pojo.TokenEntity;
import com.lxe.lx.service.AiGradingService;
import com.lxe.lx.service.HomeworkSubmissionService;
import com.lxe.lx.util.ResultConstant;
import com.lxe.lx.util.Tools;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static com.lxe.lx.config.AuthorizationInterceptor.ORG_ID_KEY;

@RestController
@RequestMapping("/homework/submission")
public class HomeworkSubmissionController {
    Logger logger = LogManager.getLogger(HomeworkSubmissionController.class);

    @Autowired
    private HomeworkSubmissionService homeworkSubmissionService;

    @Autowired
    private AiGradingService aiGradingService;

    @Autowired
    @Qualifier("aiTaskExecutor")
    private TaskExecutor aiTaskExecutor;

    /**
     * 学生提交作业
     */
    @Login
    @RequestMapping(value = "/add", method = RequestMethod.POST)
    public ResultConstant add(HttpServletRequest request, @RequestBody HomeworkSubmission submission) {
        if (submission == null || StringUtils.isBlank(submission.getAssignmentId())) {
            return ResultConstant.illegalParams("作业ID不能为空");
        }
        // 允许内容和文件地址为空，但至少要有一个
        if (StringUtils.isBlank(submission.getContent()) && StringUtils.isBlank(submission.getFileAddress())) {
            return ResultConstant.illegalParams("作业内容或附件至少提供一个");
        }
        try {
            TokenEntity tokenEntity = (TokenEntity) request.getAttribute(ORG_ID_KEY);
            
            // 检查是否已经提交过
            HomeworkSubmission existing = homeworkSubmissionService.getByAssignmentAndStudent(
                submission.getAssignmentId(), tokenEntity.getId()
            );
            if (existing != null) {
                return ResultConstant.error("您已经提交过该作业，请使用编辑功能修改");
            }
            
            submission.setId(Tools.get32UUID());
            submission.setStudentId(tokenEntity.getId());
            submission.setStudentName(tokenEntity.getName());  // 设置学生姓名
            submission.setCreateId(tokenEntity.getId());
            submission.setCreateTime(Tools.nowTimeStr());
            submission.setSubmitTime(Tools.nowTimeStr());
            submission.setStatus("submitted");
            submission.setState("1");
            submission.setVersion(1);
            
            ResultConstant ref = homeworkSubmissionService.add(submission);
            return ref;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("add->error" + e.getMessage());
            return ResultConstant.error("提交作业失败");
        }
    }

    /**
     * 学生编辑已提交的作业
     */
    @Login
    @RequestMapping(value = "/edit", method = RequestMethod.POST)
    public ResultConstant edit(HttpServletRequest request, @RequestBody HomeworkSubmission submission) {
        if (submission == null || StringUtils.isBlank(submission.getId())) {
            return ResultConstant.illegalParams("提交ID不能为空");
        }
        try {
            TokenEntity tokenEntity = (TokenEntity) request.getAttribute(ORG_ID_KEY);
            HomeworkSubmission temp = homeworkSubmissionService.getById(submission.getId());
            if (temp == null) {
                return ResultConstant.error("提交记录不存在");
            }
            if (isStudent(tokenEntity) && !tokenEntity.getId().equals(temp.getStudentId())) {
                return ResultConstant.notAuthorized();
            }
            if (!temp.getVersion().equals(submission.getVersion())) {
                return ResultConstant.error("数据已修改，请刷新后重试");
            }
            // 已批改的作业不能修改
            if ("graded".equals(temp.getStatus())) {
                return ResultConstant.error("作业已批改，不能修改");
            }
            
            submission.setUpdateId(tokenEntity.getId());
            submission.setUpdateTime(Tools.nowTimeStr());
            submission.setSubmitTime(Tools.nowTimeStr()); // 更新提交时间
            submission.setStatus("submitted"); // 确保状态为已提交
            
            ResultConstant ref = homeworkSubmissionService.edit(submission);
            return ref;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("edit->error" + e.getMessage());
            return ResultConstant.error("编辑作业失败");
        }
    }

    /**
     * 教师批改作业
     */
    @Login
    @TeacherOnly
    @RequestMapping(value = "/grade", method = RequestMethod.POST)
    public ResultConstant grade(HttpServletRequest request, @RequestBody HomeworkSubmission submission) {
        if (submission == null || StringUtils.isBlank(submission.getId())) {
            return ResultConstant.illegalParams("提交ID不能为空");
        }
        if (submission.getGrade() == null) {
            return ResultConstant.illegalParams("成绩不能为空");
        }
        try {
            TokenEntity tokenEntity = (TokenEntity) request.getAttribute(ORG_ID_KEY);
            HomeworkSubmission temp = homeworkSubmissionService.getById(submission.getId());
            if (temp == null) {
                return ResultConstant.error("提交记录不存在");
            }
            if (!temp.getVersion().equals(submission.getVersion())) {
                return ResultConstant.error("数据已修改，请刷新后重试");
            }
            
            submission.setGradedBy(tokenEntity.getId());
            submission.setGradedTime(Tools.nowTimeStr());
            submission.setUpdateId(tokenEntity.getId());
            submission.setUpdateTime(Tools.nowTimeStr());
            
            ResultConstant ref = homeworkSubmissionService.gradeHomework(submission);
            // BE-06 AI 批改增强：手动批改成功后，异步触发 AI 批改（best-effort，失败不影响主流程）
            if (ref.getStatus() == ResultConstant.SUCCESS) {
                aiTaskExecutor.execute(() -> aiGradingService.grade(submission));
            }
            return ref;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("grade->error" + e.getMessage());
            return ResultConstant.error("批改作业失败");
        }
    }

    /**
     * 查询提交详情
     */
    @Login
    @RequestMapping(value = "/detail", method = RequestMethod.POST)
    public ResultConstant detail(HttpServletRequest request, String id) {
        try {
            TokenEntity tokenEntity = (TokenEntity) request.getAttribute(ORG_ID_KEY);
            HomeworkSubmission submission = homeworkSubmissionService.getById(id);
            if (submission == null) {
                return ResultConstant.error("提交记录不存在");
            }
            if (isStudent(tokenEntity) && !tokenEntity.getId().equals(submission.getStudentId())) {
                return ResultConstant.notAuthorized();
            }
            return ResultConstant.success(submission);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("detail->error" + e.getMessage());
            return ResultConstant.error("查询失败");
        }
    }

    /**
     * 查询学生针对某个作业的提交记录
     */
    @Login
    @RequestMapping(value = "/getByAssignment", method = RequestMethod.POST)
    public ResultConstant getByAssignment(HttpServletRequest request, String assignmentId) {
        if (StringUtils.isBlank(assignmentId)) {
            return ResultConstant.illegalParams("作业ID不能为空");
        }
        try {
            TokenEntity tokenEntity = (TokenEntity) request.getAttribute(ORG_ID_KEY);
            HomeworkSubmission submission = homeworkSubmissionService.getByAssignmentAndStudent(
                assignmentId, tokenEntity.getId()
            );
            return ResultConstant.success(submission);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("getByAssignment->error" + e.getMessage());
            return ResultConstant.error("查询失败");
        }
    }

    /**
     * 删除提交
     */
    @Login
    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    public ResultConstant delete(HttpServletRequest request, String id) {
        if (StringUtils.isBlank(id)) {
            return ResultConstant.illegalParams("id不能为空");
        }
        try {
            TokenEntity tokenEntity = (TokenEntity) request.getAttribute(ORG_ID_KEY);
            HomeworkSubmission submission = homeworkSubmissionService.getById(id);
            if (submission == null) {
                return ResultConstant.error("提交记录不存在");
            }
            if (isStudent(tokenEntity) && !tokenEntity.getId().equals(submission.getStudentId())) {
                return ResultConstant.notAuthorized();
            }
            ResultConstant ref = homeworkSubmissionService.delete(id);
            return ref;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("delete->error" + e.getMessage());
            return ResultConstant.error("删除失败");
        }
    }

    /**
     * 查询提交列表
     */
    @Login
    @RequestMapping(value = "/list", method = RequestMethod.POST)
    public ResultConstant list(HttpServletRequest request, @RequestBody HomeworkSubmissionQO submissionQO) throws Exception {
        TokenEntity tokenEntity = (TokenEntity) request.getAttribute(ORG_ID_KEY);
        if (isStudent(tokenEntity)) {
            submissionQO.setStudentId(tokenEntity.getId());
        }
        ValidDTO validDTO = submissionQO.validPageParams(submissionQO);
        if (!validDTO.getResult()) {
            return ResultConstant.illegalParams(validDTO.getMsg());
        }
        try {
            HomeworkSubmissionDTO submissionDTO = new HomeworkSubmissionDTO();
            int count = homeworkSubmissionService.num(submissionQO);
            submissionDTO.setCount(count);
            if (count > 0) {
                List<HomeworkSubmission> list = homeworkSubmissionService.list(submissionQO);
                submissionDTO.setList(list);
            }
            return ResultConstant.success(submissionDTO);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("list->error" + e.getMessage());
            return ResultConstant.error("查询失败");
        }
    }

    private boolean isStudent(TokenEntity tokenEntity) {
        return tokenEntity != null && TokenEntity.ROLE_STUDENT.equals(tokenEntity.getRole());
    }
}


