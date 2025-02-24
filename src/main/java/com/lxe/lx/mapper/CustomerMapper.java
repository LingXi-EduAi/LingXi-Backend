package com.lxe.lx.mapper;

import com.lxe.lx.pojo.Customer;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

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

}
