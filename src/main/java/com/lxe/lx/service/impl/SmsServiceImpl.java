package com.lxe.lx.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.teaopenapi.models.OpenApiRequest;
import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.auth.BasicCredentials;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;
import com.aliyuncs.http.HttpClientConfig;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import com.lxe.lx.service.SmsService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.aliyun.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class SmsServiceImpl implements SmsService {
    @Value("${aliyun.sms.accessKeyId}")
    private String accessKeyId;
    @Value("${aliyun.sms.accessKeySecret}")
    private String accessKeySecret;

    @Override
    public boolean send(Map<String, Object> param, String phoneNumber) throws IOException {
        if (StringUtils.isEmpty(phoneNumber)) {
            return false;
        }

        DefaultProfile profile = DefaultProfile.getProfile("cn-hangzhou", accessKeyId, accessKeySecret);//自己账号的AccessKey信息
        IAcsClient client = new DefaultAcsClient(profile);

        CommonRequest request = new CommonRequest();
        request.setSysMethod(MethodType.POST);
        request.setSysDomain("dysmsapi.aliyuncs.com");//短信服务的服务接入地址

        request.setSysVersion("2017-05-25");//API的版本号
        request.setSysAction("SendSms");//API的名称

        request.putQueryParameter("PhoneNumbers", phoneNumber);//接收短信的手机号码
        request.putQueryParameter("SignName", "灵犀");//短信签名名称
        request.putQueryParameter("TemplateCode", "SMS_478995206");//短信模板ID
        request.putQueryParameter("TemplateParam", JSONObject.toJSONString(param));//短信模板变量对应的实际值

        try {
            CommonResponse response = client.getCommonResponse(request);
            System.out.println(response.getData());
            boolean success = response.getHttpResponse().isSuccess();
            return success;
        } catch (ServerException e) {
            e.printStackTrace();
        } catch (ClientException e) {
            e.printStackTrace();
        }

        return false;
//    }

//        String to =phone+"@qq.com";//收件人
//        System.out.println("向"+to+"发送邮件");
//        String from = "3150847886@qq.com";//发送人邮箱必须开启..？？？.服务
//        String subject = "验证码邮件";
//        String context = "阿里云短信服务没钱用====>验证码"+param;
//        SimpleMailMessage message = new SimpleMailMessage();
//        message.setFrom(from);
//        message.setText(context);
//        message.setTo(to);
//        message.setSubject(subject);
//        javaMailSender.send(message);
//        System.out.println("成功发送邮件---->");
//        return true;

    }
}

