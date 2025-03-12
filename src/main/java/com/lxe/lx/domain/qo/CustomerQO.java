package com.lxe.lx.domain.qo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerQO extends BaseListQO{
    private String name;
    private String classId;
    private String state;
    /**
     * 是否进行分页0-不分1-分
     */
    private String pageType;
}
