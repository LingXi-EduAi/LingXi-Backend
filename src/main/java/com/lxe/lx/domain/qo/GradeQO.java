package com.lxe.lx.domain.qo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GradeQO extends BaseListQO{
    private double grade;
    private String classId;
    private String studentId;
    private String subject;
    private String week;
    private String unit;
    private String evaluate;
    private String state;
    /**
     * 是否进行分页0-不分1-分
     */
    private String pageType;
}
