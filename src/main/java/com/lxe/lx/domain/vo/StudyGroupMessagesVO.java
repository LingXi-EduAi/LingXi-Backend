package com.lxe.lx.domain.vo;

import com.lxe.lx.pojo.StudyGroupMessage;
import lombok.Data;

import java.util.List;

/**
 * 学习小组消息列表 VO
 * <p>
 * 对应 /studyGroup/messages 返回 data 的 JSON 结构（list）。
 */
@Data
public class StudyGroupMessagesVO {

    /** 消息列表 */
    private List<StudyGroupMessage> list;
}