package com.lxe.lx.service;

import com.lxe.lx.domain.qo.CustomerQO;
import com.lxe.lx.domain.qo.HomeworkQO;
import com.lxe.lx.pojo.Customer;
import com.lxe.lx.pojo.Homework;
import com.lxe.lx.util.ResultConstant;

import java.util.List;

public interface HomeworkService {
    public ResultConstant add(Homework homework)throws Exception;
    public ResultConstant edit(Homework homework)throws Exception;
    public Homework getHomeworkById(String id)throws Exception;
    public ResultConstant delete(String id)throws Exception;
    public int num(HomeworkQO homeworkQO)throws Exception;
    public List<Homework> list(HomeworkQO homeworkQO)throws Exception;
}
