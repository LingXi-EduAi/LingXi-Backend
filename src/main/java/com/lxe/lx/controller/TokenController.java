package com.lxe.lx.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.lxe.lx.annotation.Login;
import com.lxe.lx.pojo.Customer;
import com.lxe.lx.pojo.TokenEntity;
import com.lxe.lx.service.CustomerService;
import com.lxe.lx.service.TokenService;
import com.lxe.lx.util.MD5;
import com.lxe.lx.util.ResultConstant;
import com.lxe.lx.util.Tools;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import static com.lxe.lx.config.AuthorizationInterceptor.ORG_ID_KEY;

@RestController
@RequestMapping("/token")
public class TokenController {
    Logger logger = LogManager.getLogger(TokenController.class);

    @Autowired
    private CustomerService customerService;
    @Autowired
    private TokenService tokenService;
    @Autowired
    private RedisTemplate<String, TokenEntity> redisTemplate;

    private static final String TOKEN_PREFIX = "token:";

    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public ResultConstant login(HttpServletRequest request, @RequestBody Customer customer) {
//    public ResultConstant login(HttpServletRequest request, Customer customer) {

        if (customer == null || StringUtils.isBlank(customer.getUserId())) {
            return ResultConstant.illegalParams("用户名不能为空");
        } else if (StringUtils.isBlank(customer.getPassword())) {
            return ResultConstant.illegalParams("密码不能为空");
        }
        try {
            int loginType = 0;
            if (Tools.isValidEmail(customer.getUserId())) {
                loginType = 1;
            } else if (Tools.checkMobileNumber(customer.getUserId())) {
                loginType = 2;
            }

            Customer customerTemp = new Customer();
            if (loginType == 0) {
                customerTemp = customerService.getCustomerByUserId(customer.getUserId());
            } else if (loginType == 1) {
                customerTemp = customerService.getCustomerByEmail(customer.getUserId());
            } else if (loginType == 2) {
                customerTemp = customerService.getCustomerByPhoneNumber(customer.getUserId());
            }

            if (customerTemp == null) {
                return ResultConstant.error("人员不存在");
            }
            if ("0".equals(customerTemp.getState())) {
                return ResultConstant.error("账号已停用");
            }

            if (customerTemp.getPassword().equalsIgnoreCase(MD5.md5(customer.getPassword()))) {
                // 生成 Token
                String token = Tools.get32UUID();
                String time = Tools.nowTimeStr();
                String ip = Tools.getIp(request);

                TokenEntity tokenEntity = new TokenEntity();
                tokenEntity.setToken(token);
                tokenEntity.setId(customerTemp.getId());
                tokenEntity.setUserId(customerTemp.getUserId());
                tokenEntity.setName(customerTemp.getName());
                tokenEntity.setUpdateTime(time);
                tokenEntity.setIp(ip);
                tokenEntity.setState("1");

                // Redis 操作对象
                ValueOperations<String, TokenEntity> ops = redisTemplate.opsForValue();

                // 先检查 Redis 里是否已经有该用户的 Token（表示已登录）
                String redisKey = "token:" + token;
//                TokenEntity existingToken = ops.get(redisKey);
                Object obj = ops.get(redisKey);
                TokenEntity existingToken = new TokenEntity();
                if (obj instanceof LinkedHashMap) {
                    // 使用 ObjectMapper 进行转换
                    ObjectMapper objectMapper = new ObjectMapper();
                    existingToken = objectMapper.convertValue(obj, TokenEntity.class);
                } else if (obj instanceof TokenEntity) {
                    existingToken = (TokenEntity) obj;
                }

                if (existingToken != null) {
                    // 如果需要异地登录，则删除旧 Token
                    redisTemplate.delete(redisKey);
                }

                // 存入 Redis，并设置过期时间（例如 30 分钟）
                ops.set(redisKey, tokenEntity, 30, TimeUnit.MINUTES);

                return ResultConstant.success(token);
            } else {
                return ResultConstant.error("密码错误");
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("login->error" + e.getMessage());
            return ResultConstant.error("登录失败");
        }
    }

    //    public ResultConstant login(HttpServletRequest request, @RequestBody Customer customer) {
//        if (customer == null || StringUtils.isBlank(customer.getUserId())) {
//            return ResultConstant.illegalParams("用户名不能为空");
//        } else if (StringUtils.isBlank(customer.getPassword())) {
//            return ResultConstant.illegalParams("密码不能为空");
//        }
//        try {
//            int loginType=0;
//            if(Tools.isValidEmail(customer.getUserId())){
//                loginType=1;
//            }else if(Tools.checkMobileNumber(customer.getUserId())){
//                loginType=2;
//            }
//            Customer customerTemp = new Customer();
//            if (loginType==0) {
//                customerTemp = customerService.getCustomerByUserId(customer.getUserId());
//            }else if(loginType==1){
//                customerTemp = customerService.getCustomerByEmail(customer.getUserId());
//            }else if(loginType==2){
//                customerTemp = customerService.getCustomerByPhoneNumber(customer.getUserId());
//            }
//            String token = "";
//            if (customerTemp == null) {
//                return ResultConstant.error("人员不存在");
//            }
//            if ("0".equals(customerTemp.getState())) {
//                return ResultConstant.error("账号已停用");
//            }
//            if (customerTemp.getPassword().equalsIgnoreCase(MD5.md5(customer.getPassword()))) {
//                token = Tools.get32UUID();
//                String time = Tools.nowTimeStr();
//                TokenEntity tokenEntity = new TokenEntity();
//                tokenEntity.setToken(token);
//                tokenEntity.setId(customerTemp.getId());
//                tokenEntity.setUserId(customerTemp.getUserId());
//                tokenEntity.setName(customerTemp.getName());
//                tokenEntity.setUpdateTime(time);
//                //设置登录ip
//                String ip = Tools.getIp(request);
//                tokenEntity.setIp(ip);
//                tokenEntity.setState("1");
//                /**
//                 * 如果异地登录的话应该让后登录者进入系统
//                 */
//                TokenEntity tempTokenEntity = tokenService.getEntityById(customerTemp.getId());
//                if(tempTokenEntity != null){
////                    return ResultConstant.error("账户已经登录");
//                    int temp = tokenService.deleteById(customerTemp.getId());
//                    if(temp>0){
//                        tokenService.insert(tokenEntity);
//                        return ResultConstant.success(token);
//                    }
//                    return ResultConstant.error("登录失败");
//                }
//                tokenService.insert(tokenEntity);
//                return ResultConstant.success(token);
//            }else{
//                return ResultConstant.error("密码错误");
//            }
//        }catch (Exception e){
//            e.printStackTrace();
//            logger.error("login->error"+e.getMessage());
//
//            return ResultConstant.error("登录失败");
//        }
//
//    }
//    @Login
//    @RequestMapping(value = "/logout", method = RequestMethod.POST)
//    public ResultConstant logout(HttpServletRequest request, String token) {
//        try {
//            if (StringUtils.isBlank(token)) {
//                // 从请求头获取token
//                token = request.getHeader("token");
//                // 如果请求头没有token,则从请求参数中取
//                if (StringUtils.isBlank(token)) {
//                    token = request.getParameter("token");
//                }
//            }
////            移除属性
//            request.removeAttribute(ORG_ID_KEY);
//            //数据库删除该token
//            int re = tokenService.deleteByToken(token);
//            if(re>0){
//                return ResultConstant.success("");
//            }
//            return ResultConstant.error("token错误");
//        } catch (Exception e) {
//            e.printStackTrace();
//            logger.error("logout->error"+e.getMessage());
//
//            return ResultConstant.success("");
//        }
//    }
    @Login
    @PostMapping("/logout")
    public ResultConstant logout(HttpServletRequest request, @RequestParam(required = false) String token) {
        try {
            // 获取 token（优先从参数获取，再从请求头获取）
            if (StringUtils.isBlank(token)) {
                token = request.getHeader("token");
                if (StringUtils.isBlank(token)) {
                    token = request.getParameter("token");
                }
            }

            if (StringUtils.isBlank(token)) {
                return ResultConstant.error("缺少 token 值");
            }

            // 移除 request 中的 token 信息
            request.removeAttribute(ORG_ID_KEY);

            // 🚀 直接从 Redis 删除 Token
            Boolean deleted = redisTemplate.delete(TOKEN_PREFIX + token);
            if (Boolean.TRUE.equals(deleted)) {
                return ResultConstant.success("登出成功");
            }
            return ResultConstant.error("无效的 token 或已过期");

        } catch (Exception e) {
            e.printStackTrace();
            logger.error("logout -> error: " + e.getMessage());
            return ResultConstant.error("登出失败");
        }
    }
}
