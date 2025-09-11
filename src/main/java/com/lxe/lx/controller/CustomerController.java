package com.lxe.lx.controller;

import com.lxe.lx.annotation.Login;
import com.lxe.lx.domain.dto.ClassGroupingDTO;
import com.lxe.lx.domain.dto.CustomerDTO;
import com.lxe.lx.domain.dto.ValidDTO;
import com.lxe.lx.domain.qo.ClassGroupingQO;
import com.lxe.lx.domain.qo.CustomerQO;
import com.lxe.lx.pojo.ClassGrouping;
import com.lxe.lx.pojo.Customer;
import com.lxe.lx.pojo.TokenEntity;
import com.lxe.lx.service.CustomerService;
import com.lxe.lx.service.TokenService;
import com.lxe.lx.util.ResultConstant;
import com.lxe.lx.util.Tools;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import static com.lxe.lx.config.AuthorizationInterceptor.ORG_ID_KEY;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

import com.lxe.lx.util.MD5;

import java.util.List;

@RestController
@RequestMapping("/customer")
public class CustomerController {
    Logger logger = LogManager.getLogger(CustomerController.class);
    @Autowired
    private CustomerService customerService;
    @Autowired
    private TokenService tokenService;
    @Autowired
    private RedisTemplate<String, String> smsRedisTemplate;
    @Autowired
    private RedisTemplate<String, TokenEntity> redisTemplate;

    @RequestMapping(value = "/add", method = RequestMethod.POST)
    public ResultConstant add(HttpServletRequest request, @RequestBody Customer customer) {
//    public ResultConstant add(HttpServletRequest request, Customer customer) {
        if (customer == null || StringUtils.isBlank(customer.getUserId())) {
            return ResultConstant.illegalParams("账号不能为空");
        }else if(Tools.isValidEmail(customer.getUserId())){
            return ResultConstant.illegalParams("用户名不能包含电话");
        }else if(Tools.checkMobileNumber(customer.getUserId())){
            return ResultConstant.illegalParams("用户名不能包含邮箱");
        }else if(customer.getUserId().length()>32){
            return ResultConstant.illegalParams("账号过长，不能超过32位");
        }
        else if(customer.getName().length()>32) {
            return ResultConstant.illegalParams("姓名过长，不能超过32位");
        }
        else if (StringUtils.isBlank(customer.getName())){
            return ResultConstant.illegalParams("姓名为空");
        }
        else if (StringUtils.isBlank(customer.getPhoneNumber())){
            return ResultConstant.illegalParams("号码为空");
        } else if (StringUtils.isBlank(customer.getEmail())){
            return ResultConstant.illegalParams("邮箱为空");
        }else if(!Tools.isValidEmail(customer.getEmail())){
            return ResultConstant.illegalParams("邮箱格式错误");
        }
        else if (StringUtils.isBlank(customer.getPassword())) {
            return ResultConstant.illegalParams("密码不能为空");
        }else if(!Tools.checkMobileNumber(customer.getPhoneNumber())){
            return ResultConstant.illegalParams("号码格式错误");
        }else if (StringUtils.isBlank(customer.getState())) {
            return ResultConstant.illegalParams("状态不能为空");
        }
//        else if (StringUtils.isBlank(customer.getName())) {
//            return ResultConstant.illegalParams("用户名不能为空");
//        }
        try{
            Customer temp = customerService.getCustomerByUserId(customer.getUserId());
            if(temp!=null && !"0".equals(temp.getState())){
                return ResultConstant.illegalParams("已存在相同账号");
            }
            Customer tempEmail = customerService.getCustomerByEmail(customer.getEmail());
            if(tempEmail!=null && !"0".equals(tempEmail.getState())){
                return ResultConstant.illegalParams("已存在相同邮箱");
            }
            Customer tempPhoneNumber = customerService.getCustomerByPhoneNumber(customer.getPhoneNumber());
            if(tempPhoneNumber!=null && !"0".equals(tempPhoneNumber.getState())){
                return ResultConstant.illegalParams("已存在相同电话号码");
            }
            customer.setId(Tools.get32UUID());
            customer.setCreateTime(Tools.nowTimeStr());
            // 确保状态正确设置：1=教师, 2=学生
            if (StringUtils.isBlank(customer.getState())) {
                customer.setState("2"); // 默认为学生
            }
            // 验证状态值的合法性
            if (!"1".equals(customer.getState()) && !"2".equals(customer.getState())) {
                return ResultConstant.illegalParams("用户角色选择无效");
            }
            customer.setVersion(1);
            customer.setPassword(MD5.md5(customer.getPassword()));
            ResultConstant ref = customerService.add(customer);
            return ref;
        }catch (Exception e){
            e.printStackTrace();
            logger.error("add->error"+e.getMessage());

            return ResultConstant.error("新建账号失败");
        }
    }
    @Login
    @RequestMapping(value = "/edit", method = RequestMethod.POST)
    public ResultConstant edit(HttpServletRequest request,@RequestBody Customer customer) {
//    public ResultConstant edit(HttpServletRequest request,Customer customer) {
        if (customer == null || StringUtils.isBlank(customer.getUserId())) {
            return ResultConstant.illegalParams("账号不能为空");
        }else if(Tools.isValidEmail(customer.getUserId())){
            return ResultConstant.illegalParams("用户名不能包含电话");
        }else if(Tools.checkMobileNumber(customer.getUserId())){
            return ResultConstant.illegalParams("用户名不能包含邮箱");
        }else if(customer.getUserId().length()>32){
            return ResultConstant.illegalParams("账号过长");
        }
        else if(customer.getName().length()>32) {
            return ResultConstant.illegalParams("姓名过长");
        }
//        else if (StringUtils.isBlank(customer.getPassword())) {
//            return ResultConstant.illegalParams("密码不能为空");
//        }
        else if (StringUtils.isBlank(customer.getName())){
            return ResultConstant.illegalParams("姓名为空");
        }
        else if(!Tools.checkMobileNumber(customer.getPhoneNumber())){
            return ResultConstant.illegalParams("号码格式错误");
        }else if (StringUtils.isBlank(customer.getPhoneNumber())){
            return ResultConstant.illegalParams("号码为空");
        }else if (StringUtils.isBlank(customer.getEmail())){
            return ResultConstant.illegalParams("邮箱为空");
        }else if(!Tools.isValidEmail(customer.getEmail())){
            return ResultConstant.illegalParams("邮箱格式错误");
        }
        try{
            TokenEntity tokenEntity = (TokenEntity)request.getAttribute(ORG_ID_KEY);
            Customer temp = customerService.getCustomerById(customer.getId());
            if(temp==null){
                return ResultConstant.error("当前账户不存在");
            }
            int intTemp = customerService.countByUserIdAndNoId(customer);
            int intTempEmail = customerService.countByEmailAndNoId(customer);
            int intTempPhoneNumber = customerService.countByPhoneNumberAndNoId(customer);
            if(intTemp==1){
                return ResultConstant.illegalParams("已存在相同账号");
            }else if(intTempEmail==1){
                return ResultConstant.illegalParams("邮箱已注册");
            }else if(intTempPhoneNumber==1){
                return ResultConstant.illegalParams("手机号已注册");
            } else if(!temp.getVersion().equals(customer.getVersion())){
                return ResultConstant.error("数据已修改，请刷新后重试");
            }
//            else if("0".equals(temp.getState())){
//                return ResultConstant.error("账号已停用无法修改");
//            }
            customer.setUpdateTime(Tools.nowTimeStr());
            customer.setUpdateId(tokenEntity.getId());
            ResultConstant ref = customerService.edit(customer);
            return ref;
        }catch (Exception e){
            e.printStackTrace();
            logger.error("edit->error"+e.getMessage());

            return ResultConstant.error("修改失败");
        }
    }

    @RequestMapping(value = "/editPassword", method = RequestMethod.POST)
    public ResultConstant editPassword(HttpServletRequest request,@RequestBody Customer customer,String code) {
//    public ResultConstant editPassword(HttpServletRequest request,Customer customer,String code) {

//        if (customer == null || StringUtils.isBlank(customer.getUserId())) {
//            return ResultConstant.illegalParams("账号不能为空");
//        }else
        if (customer == null||StringUtils.isBlank(customer.getPhoneNumber())){
            return ResultConstant.illegalParams("号码为空");
        }else if(!Tools.checkMobileNumber(customer.getPhoneNumber())){
            return ResultConstant.illegalParams("号码格式错误");
        }else if (StringUtils.isBlank(code)){
            return ResultConstant.illegalParams("验证码为空");
        }else if (StringUtils.isBlank(customer.getPassword())){
            return ResultConstant.illegalParams("密码为空");
        }
        try {
//            Customer temp = customerService.getCustomerByUserId(customer.getUserId());
//            if (temp == null) {
//                return ResultConstant.error("当前账户不存在");
//            }
//            if (!customer.getPhoneNumber().equals(temp.getPhoneNumber())) {
//                return ResultConstant.error("电话号码错误，无法修改密码");
//            }

            Customer temp = customerService.getCustomerByPhoneNumber(customer.getPhoneNumber());
            if (temp == null) {
                return ResultConstant.error("当前账户不存在");
            }
            String codeTemp = smsRedisTemplate.opsForValue().get(customer.getPhoneNumber());
            if (codeTemp==null){
                return ResultConstant.error("验证码已过期");
            }else if(!code.equals(codeTemp)){
                return ResultConstant.error("验证码错误");
            }
            temp.setPassword(MD5.md5(customer.getPassword()));
            ResultConstant ref = customerService.editPassword(temp);
            return ref;
        }catch (Exception e){
            e.printStackTrace();
            logger.error("editPassword->error"+e.getMessage());

            return ResultConstant.error("修改失败");
        }
    }






    @Login
    @RequestMapping(value = "/detail", method = RequestMethod.POST)
    public ResultConstant detail(HttpServletRequest request, @RequestParam(required = false) String id) {
        try {
            TokenEntity tokenEntity = (TokenEntity)request.getAttribute(ORG_ID_KEY);
            String customerId = StringUtils.isNotBlank(id) ? id : tokenEntity.getId();
            Customer customer = customerService.getCustomerById(customerId);
            if(customer == null){
                return ResultConstant.error("用户不存在");
            }
            return ResultConstant.success(customer);
        }catch (Exception e){
            e.printStackTrace();
            logger.error("detail->error"+e.getMessage());

            return ResultConstant.error("查询失败");
        }
    }
    @Login
    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    public ResultConstant delete(HttpServletRequest request,String token) {
        try {
            TokenEntity tokenEntity = (TokenEntity)request.getAttribute(ORG_ID_KEY);
            ResultConstant ref = customerService.delete(tokenEntity.getId());
            if (StringUtils.isBlank(token)) {
                // 从请求头获取token
                token = request.getHeader("token");
                // 如果请求头没有token,则从请求参数中取
                if (StringUtils.isBlank(token)) {
                    token = request.getParameter("token");
                }
            }
//            移除属性
            request.removeAttribute(ORG_ID_KEY);
            String redisKey = "token:" + token;
            Boolean hasKey = redisTemplate.hasKey(redisKey);
            //数据库删除该token
            if (Boolean.TRUE.equals(hasKey)) {
                // 删除 Token
                redisTemplate.delete(redisKey);
                return ResultConstant.success("删除成功");
            } else {
                return ResultConstant.success("删除成功，token注销失败");
            }
        }catch (Exception e){
            e.printStackTrace();
            logger.error("delete->error"+e.getMessage());

            return ResultConstant.error("注销失败");
        }
    }
    @Login
    @RequestMapping(value = "/list", method = RequestMethod.POST)
    public ResultConstant list(HttpServletRequest request, @RequestBody CustomerQO customerQO) throws Exception {
//    public ResultConstant list(HttpServletRequest request, CustomerQO customerQO) throws Exception {

        ValidDTO validDTO = customerQO.validPageParams(customerQO);
        if (!validDTO.getResult()) {
            return ResultConstant.illegalParams(validDTO.getMsg());
        }
        try {
            CustomerDTO customerDTO = new CustomerDTO();
            int count = customerService.num(customerQO);
            if (count > 0) {
                List<Customer> list = customerService.list(customerQO);
                customerDTO.setList(list);
            }
            return ResultConstant.success(customerDTO);
        } catch (Exception e) {
            e.printStackTrace();

            logger.error("list->error"+e.getMessage());
            return ResultConstant.error("查询失败");
        }
    }

}
