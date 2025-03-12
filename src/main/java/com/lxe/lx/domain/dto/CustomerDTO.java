package com.lxe.lx.domain.dto;

import com.lxe.lx.pojo.ClassGrouping;
import com.lxe.lx.pojo.Customer;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
@Getter
@Setter
public class CustomerDTO {
    private List<Customer> list;
}
