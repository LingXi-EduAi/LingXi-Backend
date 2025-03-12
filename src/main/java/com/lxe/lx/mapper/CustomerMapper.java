package com.lxe.lx.mapper;

import com.lxe.lx.domain.qo.ClassGroupingQO;
import com.lxe.lx.domain.qo.CustomerQO;
import com.lxe.lx.pojo.ClassGrouping;
import com.lxe.lx.pojo.Customer;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper

public interface CustomerMapper {
    void add(Customer customer);
    void edit(Customer customer);
    void editPassword(Customer customer);
    Customer getCustomerByUserId(String userId);
    Customer getCustomerByEmail(String email);
    Customer getCustomerByPhoneNumber(String phoneNumber);
    Customer getCustomerById(String id);
    int countByUserIdAndNoId(Customer customer);
    int countByEmailAndNoId(Customer customer);
    int countByPhoneNumberAndNoId(Customer customer);
    void deleteById(String id);
    int num(CustomerQO customerQO);
    List<Customer> list(CustomerQO customerQO);
    void editList(List<Customer> studentList);
}
