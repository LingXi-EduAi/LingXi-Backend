package com.lxe.lx.pojo;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class LXClass {
    private String id;
    private String name;
    private String information;
    private String notice;
    private String classGroupingId;
    private String teacherId;
    private String createId;
    private String createTime;
    private String updateId;
    private String updateTime;
    private String state;
    private Integer version;
    private List<Customer> studentList;
}

