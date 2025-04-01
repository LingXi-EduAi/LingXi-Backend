package com.lxe.lx.pojo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Conversation {
    private String id;
    private String teacherId;
    private String studentId;
    private String conversationId;
    private String createId;
    private String createTime;
    private String updateId;
    private String updateTime;
    private String state;
    private Integer version;

}
