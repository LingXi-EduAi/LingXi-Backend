package com.lxe.lx.util;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Logback 消息脱敏转换器（BE-14-1）。
 *
 * <p>在日志格式化阶段对消息正文中的手机号、身份证号、邮箱等 PII 进行掩码，
 * 使所有经 Logback 输出的日志（含通过 log4j-api 桥接的调用）自动脱敏。
 * 通过 {@code logback-spring.xml} 的 {@code %maskedMsg} 转换词启用。
 */
public class LogbackPrivacyConverter extends MessageConverter {

    @Override
    public String convert(ILoggingEvent event) {
        String message = super.convert(event);
        if (message == null) {
            return null;
        }
        String sanitized = PrivacyTextSanitizer.sanitize(message);
        return sanitized == null ? message : sanitized;
    }
}
