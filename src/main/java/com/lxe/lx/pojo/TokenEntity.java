package com.lxe.lx.pojo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TokenEntity {
    public static final String ROLE_TEACHER = "1";
    public static final String ROLE_STUDENT = "2";

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
    /**
     * Customer role: 1 = teacher, 2 = student.
     */
    private String role;
}
