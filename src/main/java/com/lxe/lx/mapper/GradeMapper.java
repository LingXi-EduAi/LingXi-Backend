package com.lxe.lx.mapper;

import com.lxe.lx.domain.qo.GradeQO;
import com.lxe.lx.domain.qo.LXClassQO;
import com.lxe.lx.pojo.Grade;
import com.lxe.lx.pojo.LXClass;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper
public interface GradeMapper {
    void add(Grade grade);
    void edit(Grade grade);
    Grade getGradeById(String id);
    int num(GradeQO gradeQO);
    List<Grade> list(GradeQO gradeQO);
    void deleteById(String id);
}
