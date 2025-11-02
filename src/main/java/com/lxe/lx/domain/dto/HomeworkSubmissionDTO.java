package com.lxe.lx.domain.dto;

import com.lxe.lx.pojo.HomeworkSubmission;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class HomeworkSubmissionDTO {
    private int count;
    private List<HomeworkSubmission> list;
}



