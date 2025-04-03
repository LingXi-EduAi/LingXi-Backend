package com.lxe.lx.mapper;

import com.lxe.lx.domain.qo.CustomerQO;
import com.lxe.lx.domain.qo.HomeworkQO;
import com.lxe.lx.pojo.Customer;
import com.lxe.lx.pojo.Homework;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper

public interface HomeworkMapper {
    void add(Homework homework);
    void edit(Homework homework);
    Homework getHomeworkById(String id);
    void deleteById(String id);
    int num(HomeworkQO homeworkQO);
    List<Homework> list(HomeworkQO homeworkQO);
}
