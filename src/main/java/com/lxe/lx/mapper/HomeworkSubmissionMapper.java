package com.lxe.lx.mapper;

import com.lxe.lx.domain.qo.HomeworkSubmissionQO;
import com.lxe.lx.pojo.HomeworkSubmission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface HomeworkSubmissionMapper {
    
    /**
     * 添加作业提交
     */
    int add(HomeworkSubmission homeworkSubmission);
    
    /**
     * 编辑作业提交
     */
    int edit(HomeworkSubmission homeworkSubmission);
    
    /**
     * 根据id查询作业提交
     */
    HomeworkSubmission getById(String id);
    
    /**
     * 根据作业id和学生id查询提交记录
     */
    HomeworkSubmission getByAssignmentAndStudent(@Param("assignmentId") String assignmentId, 
                                                   @Param("studentId") String studentId);
    
    /**
     * 删除作业提交（逻辑删除）
     */
    int delete(String id);
    
    /**
     * 批改作业
     */
    int gradeHomework(HomeworkSubmission homeworkSubmission);
    
    /**
     * 查询作业提交列表
     */
    List<HomeworkSubmission> list(HomeworkSubmissionQO qo);
    
    /**
     * 查询作业提交数量
     */
    int num(HomeworkSubmissionQO qo);
}



