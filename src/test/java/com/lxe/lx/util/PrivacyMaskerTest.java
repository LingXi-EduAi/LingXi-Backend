package com.lxe.lx.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PrivacyMaskerTest {

    @Test
    void masksPhoneKeepingFirst3AndLast4() {
        assertEquals("138****8000", PrivacyMasker.maskPhone("13800138000"));
    }

    @Test
    void masksShortPhoneFully() {
        assertEquals("****", PrivacyMasker.maskPhone("1234"));
    }

    @Test
    void returnsNullForBlankPhone() {
        assertNull(PrivacyMasker.maskPhone(null));
        assertNull(PrivacyMasker.maskPhone(""));
        assertNull(PrivacyMasker.maskPhone("   "));
    }

    @Test
    void masksIdCardKeepingFirst4AndLast4() {
        assertEquals("1101****1234", PrivacyMasker.maskIdCard("110101199003071234"));
    }

    @Test
    void masksShortIdCardFully() {
        assertEquals("****", PrivacyMasker.maskIdCard("1234"));
    }

    @Test
    void returnsNullForBlankIdCard() {
        assertNull(PrivacyMasker.maskIdCard(null));
    }

    @Test
    void masksNameKeepingFirstChar() {
        assertEquals("张*", PrivacyMasker.maskName("张三"));
        assertEquals("欧***", PrivacyMasker.maskName("欧阳娜娜"));
    }

    @Test
    void returnsNullForBlankName() {
        assertNull(PrivacyMasker.maskName(null));
        assertNull(PrivacyMasker.maskName(""));
    }

    @Test
    void masksEmailLocalPart() {
        assertEquals("zh***@example.com", PrivacyMasker.maskEmail("zhangsan@example.com"));
    }

    @Test
    void returnsNullForBlankEmail() {
        assertNull(PrivacyMasker.maskEmail(null));
        assertNull(PrivacyMasker.maskEmail("not-an-email"));
    }
}
