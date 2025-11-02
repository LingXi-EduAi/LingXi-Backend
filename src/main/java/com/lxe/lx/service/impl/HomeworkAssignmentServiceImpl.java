package com.lxe.lx.service.impl;

import com.lxe.lx.domain.qo.HomeworkAssignmentQO;
import com.lxe.lx.mapper.HomeworkAssignmentMapper;
import com.lxe.lx.pojo.HomeworkAssignment;
import com.lxe.lx.service.HomeworkAssignmentService;
import com.lxe.lx.util.ResultConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HomeworkAssignmentServiceImpl implements HomeworkAssignmentService {
    
    @Autowired
    private HomeworkAssignmentMapper homeworkAssignmentMapper;
    
    @Override
    public ResultConstant add(HomeworkAssignment homeworkAssignment) {
        try {
            int result = homeworkAssignmentMapper.add(homeworkAssignment);
            if (result > 0) {
                return ResultConstant.success("添加作业成功");
            } else {
                return ResultConstant.error("添加作业失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResultConstant.error("添加作业失败：" + e.getMessage());
        }
    }
    
    @Override
    public ResultConstant edit(HomeworkAssignment homeworkAssignment) {
        try {
            int result = homeworkAssignmentMapper.edit(homeworkAssignment);
            if (result > 0) {
                return ResultConstant.success("编辑作业成功");
            } else {
                return ResultConstant.error("编辑作业失败，请刷新后重试");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResultConstant.error("编辑作业失败：" + e.getMessage());
        }
    }
    
    @Override
    public HomeworkAssignment getById(String id) {
        return homeworkAssignmentMapper.getById(id);
    }
    
    @Override
    public ResultConstant delete(String id) {
        try {
            int result = homeworkAssignmentMapper.delete(id);
            if (result > 0) {
                return ResultConstant.success("删除作业成功");
            } else {
                return ResultConstant.error("删除作业失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResultConstant.error("删除作业失败：" + e.getMessage());
        }
    }
    
    @Override
    public ResultConstant updateStatus(HomeworkAssignment homeworkAssignment) {
        try {
            int result = homeworkAssignmentMapper.updateStatus(homeworkAssignment);
            if (result > 0) {
                return ResultConstant.success("更新状态成功");
            } else {
                return ResultConstant.error("更新状态失败，请刷新后重试");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResultConstant.error("更新状态失败：" + e.getMessage());
        }
    }
    
    @Override
    public List<HomeworkAssignment> list(HomeworkAssignmentQO qo) {
        return homeworkAssignmentMapper.list(qo);
    }
    
    @Override
    public int num(HomeworkAssignmentQO qo) {
        return homeworkAssignmentMapper.num(qo);
    }
}



