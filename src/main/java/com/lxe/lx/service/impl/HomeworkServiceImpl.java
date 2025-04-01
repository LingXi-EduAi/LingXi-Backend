package com.lxe.lx.service.impl;

import com.lxe.lx.domain.qo.CustomerQO;
import com.lxe.lx.domain.qo.HomeworkQO;
import com.lxe.lx.mapper.HomeworkMapper;
import com.lxe.lx.pojo.Customer;
import com.lxe.lx.pojo.Homework;
import com.lxe.lx.service.HomeworkService;
import com.lxe.lx.util.ResultConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("HomeworkService")
public class HomeworkServiceImpl implements HomeworkService {
    @Autowired
    private HomeworkMapper homeworkMapper;
    @Override
    public ResultConstant add(Homework homework)throws Exception{
        try{
            homeworkMapper.add(homework);
            return ResultConstant.success("");
        }catch (Exception e){
            e.printStackTrace();
            return ResultConstant.error(e.getMessage());
        }
    }
    @Override
    public ResultConstant edit(Homework homework)throws Exception{
        try{
            homeworkMapper.edit(homework);
            return ResultConstant.success("");
        }catch (Exception e){
            e.printStackTrace();
            return ResultConstant.error(e.getMessage());
        }
    }
    @Override
    public Homework getHomeworkById(String id)throws Exception{
        return homeworkMapper.getHomeworkById(id);
    }
    @Override
    public ResultConstant delete(String id)throws Exception{
        try{
            homeworkMapper.deleteById(id);
            return ResultConstant.success("");
        }catch (Exception e){
            e.printStackTrace();
            return ResultConstant.error(e.getMessage());
        }
    }
    @Override
    public int num(HomeworkQO homeworkQO)throws Exception{
        return homeworkMapper.num(homeworkQO);
    }
    @Override
    public List<Homework> list(HomeworkQO homeworkQO)throws Exception{
        return homeworkMapper.list(homeworkQO);
    }
}
