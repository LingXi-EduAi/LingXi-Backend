package com.lxe.lx.util;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResultConstant {
    // 响应业务状态
    private Integer status = 200;

    // 响应消息
    private String msg;

    // 响应中的数据
    private Object data;

    public static final int SUCCESS = 200;
    public static final int ERROR = 300;
    public static final int PARAM_ERROR = 500;//参数有误
    public static final int TOKEN_INVALID = 1000;//token过期
    public static final int UPLOAD_ERROR = 303;//文件上传返回null
    public static final int PASSWORD_IRREGULAR = 1008;//密码不符合当前策略
    public static final int NOTAUTHORIZED = 1100;//没有接口授权

    public ResultConstant(int status, String msg, Object data) {
        this.status = status;
        this.msg = msg;
        this.data = data;
    }
    public static ResultConstant init(int status, String msg, Object data) {
        return new ResultConstant(status, msg, data);
    }
    public static ResultConstant error(String msg) {
        return new ResultConstant(ERROR, msg, null);
    }
    public static ResultConstant illegalParams(String msg) {
        return new ResultConstant(PARAM_ERROR, msg, null);
    }
    public static ResultConstant success(Object data) {
        return new ResultConstant(SUCCESS, "success", data);
    }
    public static ResultConstant irregular(String msg, String token) {
        return new ResultConstant(PASSWORD_IRREGULAR, msg, token);
    }
    public static ResultConstant notAuthorized() {
        return new ResultConstant(NOTAUTHORIZED, "账号未授权", null);
    }
}