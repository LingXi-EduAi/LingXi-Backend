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
    private String messageType; // 消息类型: text, image, file, system
    private String replyTo; // 回复的消息ID
    private String createTime;
    private String state;
    private Integer version;
}




