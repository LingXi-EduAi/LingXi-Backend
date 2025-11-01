package com.lxe.lx.pojo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StudyGroupMessage {
    private String id;
    private String groupId;
    private String senderId;
    private String senderName; // 发送者姓名缓存
    private String content;
    private String createTime;
    private String state;
    private Integer version;
}



