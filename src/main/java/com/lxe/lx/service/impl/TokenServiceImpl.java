package com.lxe.lx.service.impl;

import com.lxe.lx.domain.qo.TokenQO;
import com.lxe.lx.mapper.TokenMapper;
import com.lxe.lx.pojo.TokenEntity;
import com.lxe.lx.service.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("TokenService")
public class TokenServiceImpl implements TokenService {
    @Autowired
    private TokenMapper tokenMapper;
    @Override
    public void insert(TokenEntity tokenEntity){
        tokenMapper.insert(tokenEntity);
    }
    @Override
    public int deleteByToken(String token)throws Exception{
        return tokenMapper.deleteByToken(token);
    }
    @Override
    public int deleteById(String id)throws Exception{
        return tokenMapper.deleteById(id);
    }
    @Override
    public TokenEntity getEntity(TokenQO tokenQO)throws Exception{
        return tokenMapper.getEntity(tokenQO);
    }
    @Override
    public TokenEntity getEntityById(String id)throws Exception{
        return tokenMapper.getEntityById(id);
    }
}