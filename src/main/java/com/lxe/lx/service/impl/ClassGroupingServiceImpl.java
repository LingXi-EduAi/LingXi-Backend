package com.lxe.lx.service.impl;

import com.lxe.lx.domain.qo.ClassGroupingQO;
import com.lxe.lx.mapper.ClassGroupingMapper;
import com.lxe.lx.mapper.CustomerMapper;
import com.lxe.lx.pojo.ClassGrouping;
import com.lxe.lx.pojo.Customer;
import com.lxe.lx.service.ClassGroupingService;
import com.lxe.lx.util.ResultConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("ClassGrouping")
public class ClassGroupingServiceImpl implements ClassGroupingService {
    @Autowired
    private ClassGroupingMapper classGroupingMapper;
    @Override
    public ResultConstant add(ClassGrouping classGrouping)throws Exception{
        try{
            classGroupingMapper.add(classGrouping);
            return ResultConstant.success("");
        }catch (Exception e){
            e.printStackTrace();
            return ResultConstant.error(e.getMessage());
        }
    }
    @Override
    public ResultConstant edit(ClassGrouping classGrouping)throws Exception{
        try{
            classGroupingMapper.edit(classGrouping);
            return ResultConstant.success("");
        }catch (Exception e){
            e.printStackTrace();
            return ResultConstant.error(e.getMessage());
        }
    }
    @Override
    public int num(ClassGroupingQO classGroupingQO)throws Exception{
        return classGroupingMapper.num(classGroupingQO);
    }
    @Override
    public List<ClassGrouping> list(ClassGroupingQO classGroupingQO)throws Exception{
        return classGroupingMapper.list(classGroupingQO);
    }
    @Override
    public ClassGrouping getClassGroupingById(String id)throws Exception{
        return classGroupingMapper.getClassGroupingById(id);
    }
    @Override
    public ResultConstant deleteById(String id)throws Exception{
        try{
            classGroupingMapper.deleteById(id);
            return ResultConstant.success("");
        }catch (Exception e){
            e.printStackTrace();
            return ResultConstant.error(e.getMessage());
        }
    }
}
