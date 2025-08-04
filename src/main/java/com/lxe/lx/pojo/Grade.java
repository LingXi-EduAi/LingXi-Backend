package com.lxe.lx.pojo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class Grade {
    private String id;
    private Integer grade;
    private String classId;
    private String studentId;
    private String subject;
    private String week;
    private String unit;
    private String evaluate;
    private String createId;
    private String createTime;
    private String updateId;
    private String updateTime;
    private String state;
    private Integer version;
}
