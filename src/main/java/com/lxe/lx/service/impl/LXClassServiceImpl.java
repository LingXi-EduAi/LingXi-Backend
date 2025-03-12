package com.lxe.lx.service.impl;

import com.lxe.lx.domain.qo.CustomerQO;
import com.lxe.lx.domain.qo.LXClassQO;
import com.lxe.lx.mapper.ClassGroupingMapper;
import com.lxe.lx.mapper.CustomerMapper;
import com.lxe.lx.mapper.LXClassMapper;
import com.lxe.lx.pojo.ClassGrouping;
import com.lxe.lx.pojo.Customer;
import com.lxe.lx.pojo.LXClass;
import com.lxe.lx.service.LXClassService;
import com.lxe.lx.util.ResultConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("ClassService")
public class LXClassServiceImpl implements LXClassService {
    @Autowired
    private LXClassMapper lxClassMapper;
    @Autowired
    private ClassGroupingMapper classGroupingMapper;
    @Autowired
    private CustomerMapper customerMapper;
    @Override
    public ResultConstant add(LXClass lxClass)throws Exception{
        ClassGrouping temp = classGroupingMapper.getClassGroupingById(lxClass.getClassGroupingId());
        Integer studentNum = temp.getVolume();
        if (!(lxClass.getStudentList() == null || lxClass.getStudentList().isEmpty())) {
            if (studentNum < lxClass.getStudentList().size()) {
                ResultConstant.illegalParams("学生数量过多");
            }
            customerMapper.editList(lxClass.getStudentList());
        }
        try {
            lxClassMapper.add(lxClass);
            return ResultConstant.success("");
        }catch (Exception e){
            e.printStackTrace();
            return ResultConstant.error(e.getMessage());
        }
    }
    @Override
    public ResultConstant edit(LXClass lxClass)throws Exception{
        ClassGrouping temp = classGroupingMapper.getClassGroupingById(lxClass.getClassGroupingId());
        Integer studentNum = temp.getVolume();
        if (!(lxClass.getStudentList() == null || lxClass.getStudentList().isEmpty())) {
            if (studentNum < lxClass.getStudentList().size()) {
                ResultConstant.illegalParams("学生数量过多");
            }
            customerMapper.editList(lxClass.getStudentList());
        }
        try {
            lxClassMapper.edit(lxClass);
            return ResultConstant.success("");
        }catch (Exception e){
            e.printStackTrace();
            return ResultConstant.error(e.getMessage());
        }
    }

    @Override
    public LXClass getLXClassByUserName(String name)throws Exception{
        return lxClassMapper.getLXClassByUserName(name);
    }

    @Override
    public LXClass getLXClassById(String id) throws Exception {
        return lxClassMapper.getLXClassById(id);
    }
    @Override
    public int num(LXClassQO lxClassQO)throws Exception{
        return lxClassMapper.num(lxClassQO);
    }
    @Override
    public List<LXClass> list(LXClassQO lxClassQO)throws Exception{
        return lxClassMapper.list(lxClassQO);
    }
    @Override
    public ResultConstant deleteById(String id)throws Exception{
        try{
            lxClassMapper.deleteById(id);
            CustomerQO customerQOTemp = new CustomerQO();
            customerQOTemp.setClassId(id);
            List<Customer> studentList = customerMapper.list(customerQOTemp);
            if (!(studentList == null || studentList.isEmpty())){
                for (Customer student : studentList) {
                    student.setClassId(null);
                }
                customerMapper.editList(studentList);
            }
            return ResultConstant.success("");
        }catch (Exception e){
            e.printStackTrace();
            return ResultConstant.error(e.getMessage());
        }
    }
}
