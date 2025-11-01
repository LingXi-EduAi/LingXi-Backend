package com.lxe.lx.pojo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StudyGroup {
    private String id;
    private String name;
    private String description;
    private String category;
    private Integer maxMembers;
    private String createId;
    private String createName; // 创建者姓名，从JOIN查询获得
    private String createTime;
    private String updateId;
    private String updateTime;
    private String state;
    private Integer version;
}



