package com.lxe.lx.service.impl;

import com.lxe.lx.domain.qo.CustomerQO;
import com.lxe.lx.domain.qo.GradeQO;
import com.lxe.lx.domain.qo.LXClassQO;
import com.lxe.lx.mapper.GradeMapper;
import com.lxe.lx.mapper.LXClassMapper;
import com.lxe.lx.pojo.ClassGrouping;
import com.lxe.lx.pojo.Customer;
import com.lxe.lx.pojo.Grade;
import com.lxe.lx.pojo.LXClass;
import com.lxe.lx.service.GradeService;
import com.lxe.lx.util.ResultConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("GradeService")
public class GradeServiceImpl implements GradeService {
    @Autowired
    private GradeMapper gradeMapper;
    @Override
    public ResultConstant add(Grade grade)throws Exception{
        try {
            gradeMapper.add(grade);
            return ResultConstant.success("");
        }catch (Exception e){
            e.printStackTrace();
            return ResultConstant.error(e.getMessage());
        }
    }
    @Override
    public ResultConstant edit(Grade grade)throws Exception{
        try {
            gradeMapper.edit(grade);
            return ResultConstant.success("");
        }catch (Exception e){
            e.printStackTrace();
            return ResultConstant.error(e.getMessage());
        }
    }
    @Override
    public Grade getGradeById(String id) throws Exception {
        return gradeMapper.getGradeById(id);
    }
    @Override
    public int num(GradeQO gradeQO)throws Exception{
        return gradeMapper.num(gradeQO);
    }
    @Override
    public List<Grade> list(GradeQO gradeQO)throws Exception{
        return gradeMapper.list(gradeQO);
    }
    @Override
    public ResultConstant deleteById(String id)throws Exception{
        try{
            gradeMapper.deleteById(id);
            return ResultConstant.success("");
        }catch (Exception e){
            e.printStackTrace();
            return ResultConstant.error(e.getMessage());
        }
    }
    @Override
    public String maxWeek(GradeQO gradeQO)throws Exception{
        return gradeMapper.maxWeek(gradeQO);
    }
    @Override
    public String maxUnit(GradeQO gradeQO)throws Exception{
        return gradeMapper.maxUnit(gradeQO);
    }
}
