package com.lxe.lx.mapper;

import com.lxe.lx.domain.qo.HomeworkAssignmentQO;
import com.lxe.lx.pojo.HomeworkAssignment;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface HomeworkAssignmentMapper {
    
    /**
     * 添加作业发布
     */
    int add(HomeworkAssignment homeworkAssignment);
    
    /**
     * 编辑作业发布
     */
    int edit(HomeworkAssignment homeworkAssignment);
    
    /**
     * 根据id查询作业发布
     */
    HomeworkAssignment getById(String id);
    
    /**
     * 删除作业发布（逻辑删除）
     */
    int delete(String id);
    
    /**
     * 更新作业状态
     */
    int updateStatus(HomeworkAssignment homeworkAssignment);
    
    /**
     * 查询作业列表
     */
    List<HomeworkAssignment> list(HomeworkAssignmentQO qo);
    
    /**
     * 查询作业数量
     */
    int num(HomeworkAssignmentQO qo);
}



