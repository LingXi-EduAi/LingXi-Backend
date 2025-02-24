package com.lxe.lx.service;

import java.util.Map;

public interface SmsService {
    public boolean send(Map<String, Object> param, String phoneNumber) throws Exception;
//    public boolean verifyCode(String phoneNumber, String inputCode) throws Exception;
}
