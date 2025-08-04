package com.lxe.lx.pojo;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
@Getter
@Setter
public class GradeAnalyse {
    private String studentId;
    private String classId;
    private double averageScore;
    private String week;
    private String unit;
    private double progressPercentage;
    private Integer studentNum;
    private List<Double> dataList;
}
