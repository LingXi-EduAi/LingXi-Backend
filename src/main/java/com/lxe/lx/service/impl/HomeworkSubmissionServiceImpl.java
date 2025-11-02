package com.lxe.lx.service.impl;

import com.lxe.lx.domain.qo.HomeworkSubmissionQO;
import com.lxe.lx.mapper.HomeworkSubmissionMapper;
import com.lxe.lx.pojo.HomeworkSubmission;
import com.lxe.lx.service.HomeworkSubmissionService;
import com.lxe.lx.util.ResultConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HomeworkSubmissionServiceImpl implements HomeworkSubmissionService {
    
    @Autowired
    private HomeworkSubmissionMapper homeworkSubmissionMapper;
    
    @Override
    public ResultConstant add(HomeworkSubmission homeworkSubmission) {
        try {
            int result = homeworkSubmissionMapper.add(homeworkSubmission);
            if (result > 0) {
                return ResultConstant.success("提交作业成功");
            } else {
                return ResultConstant.error("提交作业失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResultConstant.error("提交作业失败：" + e.getMessage());
        }
    }
    
    @Override
    public ResultConstant edit(HomeworkSubmission homeworkSubmission) {
        try {
            int result = homeworkSubmissionMapper.edit(homeworkSubmission);
            if (result > 0) {
                return ResultConstant.success("更新作业成功");
            } else {
                return ResultConstant.error("更新作业失败，请刷新后重试");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResultConstant.error("更新作业失败：" + e.getMessage());
        }
    }
    
    @Override
    public HomeworkSubmission getById(String id) {
        return homeworkSubmissionMapper.getById(id);
    }
    
    @Override
    public HomeworkSubmission getByAssignmentAndStudent(String assignmentId, String studentId) {
        return homeworkSubmissionMapper.getByAssignmentAndStudent(assignmentId, studentId);
    }
    
    @Override
    public ResultConstant delete(String id) {
        try {
            int result = homeworkSubmissionMapper.delete(id);
            if (result > 0) {
                return ResultConstant.success("删除提交成功");
            } else {
                return ResultConstant.error("删除提交失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResultConstant.error("删除提交失败：" + e.getMessage());
        }
    }
    
    @Override
    public ResultConstant gradeHomework(HomeworkSubmission homeworkSubmission) {
        try {
            int result = homeworkSubmissionMapper.gradeHomework(homeworkSubmission);
            if (result > 0) {
                return ResultConstant.success("批改作业成功");
            } else {
                return ResultConstant.error("批改作业失败，请刷新后重试");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResultConstant.error("批改作业失败：" + e.getMessage());
        }
    }
    
    @Override
    public List<HomeworkSubmission> list(HomeworkSubmissionQO qo) {
        return homeworkSubmissionMapper.list(qo);
    }
    
    @Override
    public int num(HomeworkSubmissionQO qo) {
        return homeworkSubmissionMapper.num(qo);
    }
}



