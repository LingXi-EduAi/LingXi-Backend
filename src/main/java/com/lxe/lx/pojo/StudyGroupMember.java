package com.lxe.lx.pojo;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StudyGroupMember {
    private String id;
    private String groupId;
    private String customerId;
    private String customerName; // 用户姓名，从JOIN查询获得
    private String role; // owner | member
    private String joinTime;
    private String state;
    private Integer version;
}




