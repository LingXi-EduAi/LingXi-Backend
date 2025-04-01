package com.lxe.lx.domain.qo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConversationQO extends BaseListQO{
    private String teacherId;
    private String studentId;
    private String conversationId;
    private String state;
    /**
     * 是否进行分页0-不分1-分
     */
    private String pageType;
}
