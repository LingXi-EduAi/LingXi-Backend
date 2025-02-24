package com.lxe.lx.service;

import com.lxe.lx.pojo.Customer;
import com.lxe.lx.util.ResultConstant;

public interface CustomerService {
    public ResultConstant add(Customer customer)throws Exception;
    public ResultConstant edit(Customer customer)throws Exception;
    public ResultConstant editPassword(Customer customer)throws Exception;
    public Customer getCustomerByUserId(String userId)throws Exception;
    public Customer getCustomerByEmail(String userId)throws Exception;
    public Customer getCustomerByPhoneNumber(String userId)throws Exception;
    public Customer getCustomerById(String id)throws Exception;
    public int countByUserIdAndNoId(Customer customer)throws Exception;
    public int countByEmailAndNoId(Customer customer)throws Exception;
    public int countByPhoneNumberAndNoId(Customer customer)throws Exception;
    public ResultConstant delete(String id)throws Exception;

}
