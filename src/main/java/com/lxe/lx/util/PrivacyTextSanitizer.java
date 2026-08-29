package com.lxe.lx.util;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 自由文本 PII 脱敏器。
 *
 * <p>对一段自由文本（如 AI 消息内容、用户输入、日志正文）中的手机号、
 * 身份证号、邮箱等 PII 进行掩码，供持久化或日志链路调用。
 * 纯静态、无副作用，便于单元测试。
 */
public final class PrivacyTextSanitizer {

    private PrivacyTextSanitizer() {
    }

    /** 中国大陆手机号：1 开头，第二位 3-9，共 11 位数字。 */
    private static final Pattern PHONE =
            Pattern.compile("(?<![0-9])(1[3-9][0-9]{9})(?![0-9])");

    /** 18 位身份证号（含 X/x 结尾）。 */
    private static final Pattern ID_CARD =
            Pattern.compile("(?<![0-9A-Za-z])([0-9]{17}[0-9Xx])(?![0-9A-Za-z])");

    /** 邮箱。 */
    private static final Pattern EMAIL =
            Pattern.compile("([A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,})");

    /**
     * 对文本中的 PII 进行掩码。空文本返回 {@code null}。
     */
    public static String sanitize(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        String result = maskAll(text, PHONE, PrivacyMasker::maskPhone);
        result = maskAll(result, ID_CARD, PrivacyMasker::maskIdCard);
        result = maskAll(result, EMAIL, PrivacyMasker::maskEmail);
        return result;
    }

    private static String maskAll(String text, Pattern pattern, java.util.function.Function<String, String> masker) {
        Matcher matcher = pattern.matcher(text);
        StringBuffer sb = new StringBuffer(text.length());
        while (matcher.find()) {
            String masked = masker.apply(matcher.group(1));
            if (masked == null) {
                masked = matcher.group(1);
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(masked));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
