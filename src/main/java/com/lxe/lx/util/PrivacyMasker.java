package com.lxe.lx.util;

import org.apache.commons.lang3.StringUtils;

/**
 * PII（个人身份信息）脱敏工具。
 *
 * <p>提供对手机号、身份证号、姓名、邮箱等敏感字段的掩码方法。
 * 所有方法均为纯静态、无副作用，便于单元测试与在日志/持久化链路中复用。
 * 空值或无法识别的内容返回 {@code null}，调用方据此决定是否跳过脱敏。
 */
public final class PrivacyMasker {

    private PrivacyMasker() {
    }

    /** 掩码占位符。 */
    private static final String MASK = "****";

    /**
     * 手机号脱敏：保留前 3 位与后 4 位，中间以 {@code ****} 掩码。
     * 例：{@code 13800138000} → {@code 138****8000}。
     * 长度不足 7 位时整体掩码。
     */
    public static String maskPhone(String phone) {
        if (StringUtils.isBlank(phone)) {
            return null;
        }
        String trimmed = phone.trim();
        if (trimmed.length() < 7) {
            return MASK;
        }
        return trimmed.substring(0, 3) + MASK + trimmed.substring(trimmed.length() - 4);
    }

    /**
     * 身份证号脱敏：保留前 4 位与后 4 位，中间以 {@code ****} 掩码。
     * 例：{@code 110101199003071234} → {@code 1101**********1234}。
     * 长度不足 8 位时整体掩码。
     */
    public static String maskIdCard(String idCard) {
        if (StringUtils.isBlank(idCard)) {
            return null;
        }
        String trimmed = idCard.trim();
        if (trimmed.length() < 8) {
            return MASK;
        }
        return trimmed.substring(0, 4) + MASK + trimmed.substring(trimmed.length() - 4);
    }

    /**
     * 姓名脱敏：保留首字符，其余以 {@code *} 掩码。
     * 例：{@code 张三} → {@code 张*}，{@code 欧阳娜娜} → {@code 欧**}。
     */
    public static String maskName(String name) {
        if (StringUtils.isBlank(name)) {
            return null;
        }
        String trimmed = name.trim();
        if (trimmed.length() <= 1) {
            return MASK;
        }
        StringBuilder sb = new StringBuilder(trimmed.length());
        sb.append(trimmed.charAt(0));
        for (int i = 1; i < trimmed.length(); i++) {
            sb.append('*');
        }
        return sb.toString();
    }

    /**
     * 邮箱脱敏：保留本地部分前 2 位与完整域名，本地部分其余以 {@code ***} 掩码。
     * 例：{@code zhangsan@example.com} → {@code zh***@example.com}。
     * 无法解析为合法邮箱时返回 {@code null}。
     */
    public static String maskEmail(String email) {
        if (StringUtils.isBlank(email)) {
            return null;
        }
        String trimmed = email.trim();
        int at = trimmed.indexOf('@');
        if (at <= 0 || at == trimmed.length() - 1) {
            return null;
        }
        String local = trimmed.substring(0, at);
        String domain = trimmed.substring(at);
        if (local.length() <= 2) {
            return MASK + domain;
        }
        return local.substring(0, 2) + "***" + domain;
    }
}
