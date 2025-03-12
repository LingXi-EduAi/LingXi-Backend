package com.lxe.lx.mapper;

import com.lxe.lx.domain.qo.LXClassQO;
import com.lxe.lx.pojo.LXClass;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper
public interface LXClassMapper {
    void add(LXClass lxClass);
    void edit(LXClass lxClass);
    LXClass getLXClassByUserName(String name);
    LXClass getLXClassById(String id);
    int num(LXClassQO lxClassQO);
    List<LXClass> list(LXClassQO lxClassQO);
    void deleteById(String id);
}
