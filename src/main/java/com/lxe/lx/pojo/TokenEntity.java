package com.lxe.lx.pojo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TokenEntity {
    private String id;
    private String token;
    /**
     * 账号以及用户名
     */
    private String userId;
    private String name;
    /**
     * 修改时间
     */
    private String updateTime;
    /**
     * 登录ip
     */
    private String ip;
    /**
     * 状态
     */
    private String state;
}
