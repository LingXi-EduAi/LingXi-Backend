package com.lxe.lx.mapper;

import com.lxe.lx.domain.qo.TokenQO;
import com.lxe.lx.pojo.TokenEntity;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Repository
@Mapper
public interface TokenMapper {
    void insert(TokenEntity tokenEntity);
    int deleteByToken(String token);
    int deleteById(String id);
    TokenEntity getEntity(TokenQO tokenQO);
    TokenEntity getEntityById(String id);
}
