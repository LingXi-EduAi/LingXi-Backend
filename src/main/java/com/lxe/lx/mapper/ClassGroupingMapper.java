package com.lxe.lx.mapper;

import com.lxe.lx.domain.qo.ClassGroupingQO;
import com.lxe.lx.pojo.ClassGrouping;
import com.lxe.lx.pojo.Customer;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper
public interface ClassGroupingMapper {
    void add(ClassGrouping classGrouping);
    void edit(ClassGrouping classGrouping);
    int num(ClassGroupingQO classGroupingQO);
    List<ClassGrouping> list(ClassGroupingQO classGroupingQO);
    ClassGrouping getClassGroupingById(String id);
    void deleteById(String id);
}
