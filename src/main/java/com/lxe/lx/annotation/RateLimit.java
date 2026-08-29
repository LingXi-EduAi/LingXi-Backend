package com.lxe.lx.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流注解。
 *
 * <p>标注在 Controller 方法上，按当前登录用户 + 路径 + 时间窗口限流，
 * 超过阈值返回 HTTP 429。默认宽松（每分钟 60 次/用户）。
 */
@Target(ElementType.METHOD)
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /** 窗口内允许的最大请求数。 */
    int limit() default 60;

    /** 窗口秒数。 */
    long windowSeconds() default 60L;
}
