package com.lxe.lx.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PrivacyTextSanitizerTest {

    @Test
    void masksPhoneNumbersInFreeText() {
        String input = "我的手机号是 13800138000，请查收。";
        String out = PrivacyTextSanitizer.sanitize(input);
        assertEquals("我的手机号是 138****8000，请查收。", out);
    }

    @Test
    void masksIdCardsInFreeText() {
        String input = "身份证 110101199003071234 已提交。";
        String out = PrivacyTextSanitizer.sanitize(input);
        assertEquals("身份证 1101****1234 已提交。", out);
    }

    @Test
    void masksEmailsInFreeText() {
        String input = "联系 zhangsan@example.com 获取结果。";
        String out = PrivacyTextSanitizer.sanitize(input);
        assertEquals("联系 zh***@example.com 获取结果。", out);
    }

    @Test
    void leavesTextWithoutPiiUntouched() {
        String input = "请帮我分析这道数学题。";
        assertEquals(input, PrivacyTextSanitizer.sanitize(input));
    }

    @Test
    void returnsNullForBlank() {
        assertNull(PrivacyTextSanitizer.sanitize(null));
        assertNull(PrivacyTextSanitizer.sanitize(""));
    }
}
