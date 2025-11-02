package com.lxe.lx.service;

import com.lxe.lx.domain.qo.HomeworkAssignmentQO;
import com.lxe.lx.pojo.HomeworkAssignment;
import com.lxe.lx.util.ResultConstant;

import java.util.List;

public interface HomeworkAssignmentService {
    
    ResultConstant add(HomeworkAssignment homeworkAssignment);
    
    ResultConstant edit(HomeworkAssignment homeworkAssignment);
    
    HomeworkAssignment getById(String id);
    
    ResultConstant delete(String id);
    
    ResultConstant updateStatus(HomeworkAssignment homeworkAssignment);
    
    List<HomeworkAssignment> list(HomeworkAssignmentQO qo);
    
    int num(HomeworkAssignmentQO qo);
}

