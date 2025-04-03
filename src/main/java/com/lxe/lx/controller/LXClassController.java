package com.lxe.lx.controller;
import com.lxe.lx.annotation.Login;
import com.lxe.lx.domain.dto.ClassGroupingDTO;
import com.lxe.lx.domain.dto.LXClassDTO;
import com.lxe.lx.domain.dto.ValidDTO;
import com.lxe.lx.domain.qo.ClassGroupingQO;
import com.lxe.lx.domain.qo.CustomerQO;
import com.lxe.lx.domain.qo.LXClassQO;
import com.lxe.lx.pojo.ClassGrouping;
import com.lxe.lx.pojo.Customer;
import com.lxe.lx.pojo.LXClass;
import com.lxe.lx.pojo.TokenEntity;
import com.lxe.lx.service.CustomerService;
import com.lxe.lx.service.LXClassService;
import com.lxe.lx.util.ResultConstant;
import com.lxe.lx.util.Tools;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

import static com.lxe.lx.config.AuthorizationInterceptor.ORG_ID_KEY;
@RestController
@RequestMapping("/LXClass")
public class LXClassController {
    Logger logger = LogManager.getLogger(LXClassController.class);
    @Autowired
    private LXClassService lxClassService;
    @Autowired
    private CustomerService customerService;
    @Login
    @RequestMapping(value = "/add", method = RequestMethod.POST)
        public ResultConstant add(HttpServletRequest request, @RequestBody LXClass lxClass) {
//    public ResultConstant add(HttpServletRequest request, LXClass lxClass) {
        if (lxClass == null || StringUtils.isBlank(lxClass.getName())) {
            return ResultConstant.illegalParams("班级名字不能为空");
        } else if (StringUtils.isBlank(lxClass.getClassGroupingId())) {
            return ResultConstant.illegalParams("未选择分班模版");
        } else if (StringUtils.isBlank(lxClass.getTeacherId())) {
            return ResultConstant.illegalParams("未选择班主任");
        }
//        else if (StringUtils.isBlank(classGrouping.getCondition())) {
//            return ResultConstant.illegalParams("分班条件为空");
//        }
        try{
            TokenEntity tokenEntity = (TokenEntity) request.getAttribute(ORG_ID_KEY);
            LXClass temp = lxClassService.getLXClassByUserName(lxClass.getName());
            if(temp!=null && "1".equals(temp.getState())){
                return ResultConstant.illegalParams("已存在相同班级名称");
            }
            lxClass.setId(Tools.get32UUID());

            lxClass.setCreateTime(Tools.nowTimeStr());
            lxClass.setCreateId(tokenEntity.getId());
            lxClass.setState("1");
            lxClass.setVersion(1);
            if (!(lxClass.getStudentList() == null || lxClass.getStudentList().isEmpty())){
                for (Customer student : lxClass.getStudentList()) {
                    student.setClassId(lxClass.getId());
                }
            }
            ResultConstant ref = lxClassService.add(lxClass);
            return ref;
        }catch (Exception e){
            e.printStackTrace();
            logger.error("add->error"+e.getMessage());

            return ResultConstant.error("新建班级失败");
        }
    }
    @Login
    @RequestMapping(value = "/edit", method = RequestMethod.POST)
        public ResultConstant edit(HttpServletRequest request, @RequestBody LXClass lxClass) {
//    public ResultConstant edit(HttpServletRequest request, LXClass lxClass) {
        if (lxClass == null || StringUtils.isBlank(lxClass.getName())) {
            return ResultConstant.illegalParams("班级名字不能为空");
        } else if (StringUtils.isBlank(lxClass.getClassGroupingId())) {
            return ResultConstant.illegalParams("未选择分班模版");
        } else if (StringUtils.isBlank(lxClass.getTeacherId())) {
            return ResultConstant.illegalParams("未选择班主任");
        }
        try {
            TokenEntity tokenEntity = (TokenEntity) request.getAttribute(ORG_ID_KEY);
            LXClass temp = lxClassService.getLXClassById(lxClass.getId());
            CustomerQO customerQOTemp = new CustomerQO();
            customerQOTemp.setClassId(temp.getId());
            List<Customer> studentList = customerService.list(customerQOTemp);
            if (!(studentList == null || studentList.isEmpty())){
                for (Customer student : studentList) {
                    student.setClassId(null);
                }
            }
            if (!(lxClass.getStudentList() == null || lxClass.getStudentList().isEmpty())) {
                customerService.editList(studentList);
            }
            if (temp == null) {
                return ResultConstant.error("当前班级不存在");
            }
            if (!temp.getVersion().equals(lxClass.getVersion())) {
                return ResultConstant.error("数据已修改，请刷新后重试");
            }
            lxClass.setUpdateId(tokenEntity.getId());
            lxClass.setUpdateTime(Tools.nowTimeStr());
            ResultConstant ref = lxClassService.edit(lxClass);
            return ref;
        }catch (Exception e) {
            e.printStackTrace();
            logger.error("edit->error" + e.getMessage());
            return ResultConstant.error("修改失败");
        }
    }
    @Login
    @RequestMapping(value = "/detail", method = RequestMethod.POST)
        public ResultConstant detail(HttpServletRequest request, @RequestBody LXClass lxClass) {
//    public ResultConstant detail(HttpServletRequest request, LXClass lxClass) {
        if (StringUtils.isBlank(lxClass.getId())) {
            ResultConstant.illegalParams("id不能为空");
        }
        try {
            LXClass temp = lxClassService.getLXClassById(lxClass.getId());
            CustomerQO customerQOTemp = new CustomerQO();
            customerQOTemp.setClassId(temp.getId());
            List<Customer> studentList = customerService.list(customerQOTemp);
            temp.setStudentList(studentList);
            if(temp!=null){
                return ResultConstant.success(temp);
            }else {
                return ResultConstant.error("当前班级不存在");
            }
        } catch (Exception e) {
            e.printStackTrace();

            logger.error("detail->error"+e.getMessage());
            return ResultConstant.error("查看详情失败");
        }
    }
    @Login
    @RequestMapping(value = "/list", method = RequestMethod.POST)
    public ResultConstant list(HttpServletRequest request, @RequestBody LXClassQO lxClassQO) throws Exception {
//    public ResultConstant list(HttpServletRequest request, LXClassQO lxClassQO) throws Exception {

        ValidDTO validDTO = lxClassQO.validPageParams(lxClassQO);
        if (!validDTO.getResult()) {
            return ResultConstant.illegalParams(validDTO.getMsg());
        }
        try {
            LXClassDTO lxClassDTO= new LXClassDTO();
            int count = lxClassService.num(lxClassQO);
            if (count > 0) {
                List<LXClass> list = lxClassService.list(lxClassQO);
                lxClassDTO.setList(list);
            }
            return ResultConstant.success(lxClassDTO);
        } catch (Exception e) {
            e.printStackTrace();

            logger.error("list->error"+e.getMessage());
            return ResultConstant.error("查询失败");
        }
    }
    @Login
    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    public ResultConstant delete(HttpServletResponse response, String id) throws Exception {
        if (StringUtils.isBlank(id)) {
            return ResultConstant.illegalParams("id不能为空");
        }
        try {
            LXClass temp = lxClassService.getLXClassById(id);
            if (temp == null) {
                return ResultConstant.error("当前班级不存在");
            }
            ResultConstant ref = lxClassService.deleteById(id);
            return ref;
        } catch (Exception e) {
            e.printStackTrace();

            logger.error("delete->error"+e.getMessage());
            return ResultConstant.error("删除失败");
        }
    }

}
