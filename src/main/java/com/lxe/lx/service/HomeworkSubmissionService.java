package com.lxe.lx.service;

import com.lxe.lx.domain.qo.HomeworkSubmissionQO;
import com.lxe.lx.pojo.HomeworkSubmission;
import com.lxe.lx.util.ResultConstant;

import java.util.List;

public interface HomeworkSubmissionService {
    
    ResultConstant add(HomeworkSubmission homeworkSubmission);
    
    ResultConstant edit(HomeworkSubmission homeworkSubmission);
    
    HomeworkSubmission getById(String id);
    
    HomeworkSubmission getByAssignmentAndStudent(String assignmentId, String studentId);
    
    ResultConstant delete(String id);
    
    ResultConstant gradeHomework(HomeworkSubmission homeworkSubmission);
    
    List<HomeworkSubmission> list(HomeworkSubmissionQO qo);
    
    int num(HomeworkSubmissionQO qo);
}



