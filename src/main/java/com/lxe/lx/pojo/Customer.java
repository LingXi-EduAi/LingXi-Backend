package com.lxe.lx.pojo;
import lombok.Getter;
import lombok.Setter;
@Getter
@Setter
public class Customer {
    private String id;
    private String userId;
    private String name;
    private String password;
    private String classId;
    private Integer grade;
    private Integer age;
    private String phoneNumber;
    private String email;
    private String createTime;
//    状态0--废弃1--启用表示老师2--启用表示学生
    private String updateId;
    private String updateTime;
    private String state;
    private Integer version;
}
