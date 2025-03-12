package com.lxe.lx.service;

import com.lxe.lx.domain.qo.CustomerQO;
import com.lxe.lx.domain.qo.LXClassQO;
import com.lxe.lx.pojo.ClassGrouping;
import com.lxe.lx.pojo.Customer;
import com.lxe.lx.pojo.LXClass;
import com.lxe.lx.util.ResultConstant;

import java.util.List;

public interface LXClassService {
    public ResultConstant add(LXClass lxClass)throws Exception;
    public ResultConstant edit(LXClass lxClass)throws Exception;
    public LXClass getLXClassByUserName(String name)throws Exception;
    public LXClass getLXClassById(String id)throws Exception;
    public int num(LXClassQO lxClassQO)throws Exception;
    public List<LXClass> list(LXClassQO lxClassQO)throws Exception;
    public ResultConstant deleteById(String id)throws Exception;
}
