package com.lxe.lx.controller;

import com.lxe.lx.pojo.Customer;
import com.lxe.lx.pojo.TokenEntity;
import com.lxe.lx.service.CustomerService;
import com.lxe.lx.service.SmsService;
import com.lxe.lx.util.ResultConstant;
import com.lxe.lx.util.Tools;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/sms")
public class SmsController {
    @Autowired
    private CustomerService customerService;
    @Autowired
    private SmsService smsService;
    @Autowired
    private RedisTemplate<String, String> smsRedisTemplate;
    @RequestMapping(value = "/send", method = RequestMethod.POST)
    public ResultConstant sendSms(String phoneNumber) throws Exception {


        if (StringUtils.isBlank(phoneNumber)){
            return ResultConstant.illegalParams("电话为空");
        } else if(!Tools.checkMobileNumber(phoneNumber)) {
            return ResultConstant.illegalParams("号码格式错误");
        }
        Customer tempPhoneNumber = customerService.getCustomerByPhoneNumber(phoneNumber);
        if(tempPhoneNumber==null){
            return ResultConstant.illegalParams("当前号码未注册");
        }
        //1 从redis获取验证码，如果获取到直接返回
        String code = smsRedisTemplate.opsForValue().get(phoneNumber);
        if(!StringUtils.isEmpty(code)) {
            return ResultConstant.success("");
        }

        //生成一个随机值
        code = Tools.generateCode();
        Map<String,Object> param  = new HashMap<>();
        param.put("code",code);

        //调用service发送短信的方法
        boolean isSend = smsService.send(param,phoneNumber);

        if(isSend) {
            //发送成功,把发送成功验证码放到redis里面
            //设置有效时间
            smsRedisTemplate.opsForValue().set(phoneNumber,code,5, TimeUnit.MINUTES);
            return ResultConstant.success("");
        } else {
            return ResultConstant.error("短信发送失败");
        }

    }


}
