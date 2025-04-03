package com.lxe.lx.domain.qo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HomeworkQO extends BaseListQO{
    private String name;
    private String studentId;
    private String state;
    /**
     * 是否进行分页0-不分1-分
     */
    private String pageType;
}
