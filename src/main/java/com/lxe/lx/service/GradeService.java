package com.lxe.lx.service;

import com.lxe.lx.domain.qo.GradeQO;
import com.lxe.lx.domain.qo.LXClassQO;
import com.lxe.lx.pojo.Grade;
import com.lxe.lx.pojo.LXClass;
import com.lxe.lx.util.ResultConstant;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper
public interface GradeService {
    public ResultConstant add(Grade grade)throws Exception;
    public ResultConstant edit(Grade grade)throws Exception;
    public Grade getGradeById(String id)throws Exception;
    public int num(GradeQO gradeQO)throws Exception;
    public List<Grade> list(GradeQO gradeQO)throws Exception;
    public ResultConstant deleteById(String id)throws Exception;
}
