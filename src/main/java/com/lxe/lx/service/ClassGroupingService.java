package com.lxe.lx.service;

import com.lxe.lx.domain.qo.ClassGroupingQO;
import com.lxe.lx.pojo.ClassGrouping;
import com.lxe.lx.pojo.Customer;
import com.lxe.lx.util.ResultConstant;

import java.util.List;

public interface ClassGroupingService {
    public ResultConstant add(ClassGrouping classGrouping)throws Exception;
    public ResultConstant edit(ClassGrouping classGrouping)throws Exception;
    public int num(ClassGroupingQO classGroupingQO)throws Exception;
    public List<ClassGrouping> list(ClassGroupingQO classGroupingQO)throws Exception;
    public ClassGrouping getClassGroupingById(String id)throws Exception;
    public ResultConstant deleteById(String id)throws Exception;
}
