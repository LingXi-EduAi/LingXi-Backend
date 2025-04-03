package com.lxe.lx.domain.qo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LXClassQO extends BaseListQO{
    private String name;
    private String classGroupingId;
    private String teacherId;
    private String state;
    /**
     * 是否进行分页0-不分1-分
     */
    private String pageType;
}
