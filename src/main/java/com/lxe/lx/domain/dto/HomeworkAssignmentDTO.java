package com.lxe.lx.domain.dto;

import com.lxe.lx.pojo.HomeworkAssignment;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class HomeworkAssignmentDTO {
    private int count;
    private List<HomeworkAssignment> list;
}



