package com.lxe.lx.service.impl;

import com.lxe.lx.mapper.CustomerMapper;
import com.lxe.lx.pojo.Customer;
import com.lxe.lx.service.CustomerService;
import com.lxe.lx.util.ResultConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
@Service("CustomerService")
public class CustomerServiceImpl implements CustomerService {
    @Autowired
    private CustomerMapper customerMapper;

//    @Autowired
//    private TokenMapper tokenMapper;
    @Override
    public ResultConstant add(Customer customer)throws Exception{
        try{
            customerMapper.add(customer);
            return ResultConstant.success("");
        }catch (Exception e){
            e.printStackTrace();
            return ResultConstant.error(e.getMessage());
        }
    }
    @Override
    public ResultConstant edit(Customer customer)throws Exception{
        try{
            customerMapper.edit(customer);
            return ResultConstant.success("");
        }catch (Exception e){
            e.printStackTrace();
            return ResultConstant.error(e.getMessage());
        }
    }
    @Override
    public ResultConstant editPassword(Customer customer)throws Exception{
        try{
            customerMapper.editPassword(customer);
            return ResultConstant.success("");
        }catch (Exception e){
            e.printStackTrace();
            return ResultConstant.error(e.getMessage());
        }
    }
    @Override
    public Customer getCustomerByUserId(String userId)throws Exception{
        return customerMapper.getCustomerByUserId(userId);
    }
    @Override
    public Customer getCustomerByEmail(String email)throws Exception{
        return customerMapper.getCustomerByEmail(email);
    }
    @Override
    public Customer getCustomerByPhoneNumber(String phoneNumber)throws Exception{
        return customerMapper.getCustomerByPhoneNumber(phoneNumber);
    }
    @Override
    public Customer getCustomerById(String id)throws Exception{
        return customerMapper.getCustomerById(id);
    }
    @Override
    public int countByUserIdAndNoId(Customer customer)throws Exception{
        return customerMapper.countByUserIdAndNoId(customer);
    }
    @Override
    public int countByEmailAndNoId(Customer customer)throws Exception{
        return customerMapper.countByEmailAndNoId(customer);
    }
    @Override
    public int countByPhoneNumberAndNoId(Customer customer)throws Exception{
        return customerMapper.countByPhoneNumberAndNoId(customer);
    }
    @Override
    public ResultConstant delete(String id)throws Exception{
        try{
            customerMapper.deleteById(id);
            return ResultConstant.success("");
        }catch (Exception e){
            e.printStackTrace();
            return ResultConstant.error(e.getMessage());
        }
    }
}
