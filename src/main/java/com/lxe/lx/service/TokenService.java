package com.lxe.lx.service;

import com.lxe.lx.domain.qo.TokenQO;
import com.lxe.lx.pojo.TokenEntity;

public interface TokenService {
    public void insert(TokenEntity tokenEntity)throws Exception;
    public int deleteByToken(String token)throws Exception;
    public int deleteById(String id)throws Exception;

    public TokenEntity getEntity(TokenQO tokenQO)throws Exception;
    public TokenEntity getEntityById(String id)throws Exception;

}
