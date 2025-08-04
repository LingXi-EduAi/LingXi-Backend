package com.lxe.lx.controller;
import com.lxe.lx.annotation.Login;
import com.lxe.lx.domain.dto.GradeDTO;
import com.lxe.lx.domain.dto.LXClassDTO;
import com.lxe.lx.domain.dto.ValidDTO;
import com.lxe.lx.domain.qo.GradeQO;
import com.lxe.lx.domain.qo.LXClassQO;
import com.lxe.lx.pojo.Document;
import com.lxe.lx.pojo.Grade;
import com.lxe.lx.pojo.LXClass;
import com.lxe.lx.pojo.TokenEntity;
import com.lxe.lx.service.GradeService;
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
@RequestMapping("/grade")
public class GradeController {
    Logger logger = LogManager.getLogger(GradeController.class);
    @Autowired
    private GradeService gradeService;
    @Autowired
    private LXClassService lxClassService;
    @Login
    @RequestMapping(value = "/add", method = RequestMethod.POST)
//    public ResultConstant add(HttpServletRequest request,Grade grade) {
    public ResultConstant add(HttpServletRequest request, @RequestBody Grade grade) {
        if (grade == null || StringUtils.isBlank(grade.getClassId())) {
            return ResultConstant.illegalParams("班级不能为空");
        } else if (StringUtils.isBlank(grade.getStudentId())) {
            return ResultConstant.illegalParams("学生id不能为空");
        } else if (grade.getGrade()==null) {
            return ResultConstant.illegalParams("成绩不能为空");
        }
        else if (StringUtils.isBlank(grade.getUnit())) {
            return ResultConstant.illegalParams("单元不能为空");
        }
        else if (StringUtils.isBlank(grade.getWeek())) {
            return ResultConstant.illegalParams("周数不能为空");
        }
        try{
            TokenEntity tokenEntity = (TokenEntity) request.getAttribute(ORG_ID_KEY);
            if(StringUtils.isBlank(grade.getSubject())) {
                LXClass temp = lxClassService.getLXClassById(grade.getClassId());
                grade.setSubject(temp.getSubject());
            }
            grade.setId(Tools.get32UUID());
            grade.setCreateTime(Tools.nowTimeStr());
            grade.setCreateId(tokenEntity.getId());
            grade.setState("1");
            grade.setVersion(1);
            if(grade.getGrade()>=90){
                grade.setEvaluate("0");
            }else if (grade.getGrade()>=80){
                grade.setEvaluate("1");
            }else if (grade.getGrade()>=60){
                grade.setEvaluate("2");
            }else if (grade.getGrade()>=0){
                grade.setEvaluate("3");
            }

            ResultConstant ref = gradeService.add(grade);
            return ref;
        }catch (Exception e){
            e.printStackTrace();
            logger.error("add->error"+e.getMessage());

            return ResultConstant.error("新建成绩失败");
        }
    }
    @Login
    @RequestMapping(value = "/edit", method = RequestMethod.POST)
    public ResultConstant edit(HttpServletRequest request, @RequestBody Grade grade) {
//    public ResultConstant edit(HttpServletRequest request, Grade grade) {
        if (grade == null || StringUtils.isBlank(grade.getClassId())) {
            return ResultConstant.illegalParams("班级不能为空");
        } else if (StringUtils.isBlank(grade.getStudentId())) {
            return ResultConstant.illegalParams("学生id不能为空");
        } else if (grade.getGrade()==null) {
            return ResultConstant.illegalParams("成绩不能为空");
        } else if (StringUtils.isBlank(grade.getSubject())) {
            return ResultConstant.illegalParams("学科不能为空");
        }
        try {
            TokenEntity tokenEntity = (TokenEntity) request.getAttribute(ORG_ID_KEY);
            Grade temp = gradeService.getGradeById(grade.getId());
            if (temp == null) {
                return ResultConstant.error("当前成绩不存在");
            }
            if (!temp.getVersion().equals(grade.getVersion())) {
                return ResultConstant.error("数据已修改，请刷新后重试");
            }
            grade.setUpdateId(tokenEntity.getId());
            grade.setUpdateTime(Tools.nowTimeStr());
            if(grade.getGrade()>=90){
                grade.setEvaluate("0");
            }else if (grade.getGrade()>=80){
                grade.setEvaluate("1");
            }else if (grade.getGrade()>=60){
                grade.setEvaluate("2");
            }else if (grade.getGrade()>=0){
                grade.setEvaluate("3");
            }
            ResultConstant ref = gradeService.edit(grade);
            return ref;
        }catch (Exception e) {
            e.printStackTrace();
            logger.error("edit->error" + e.getMessage());
            return ResultConstant.error("修改失败");
        }
    }

    @Login
    @RequestMapping(value = "/list", method = RequestMethod.POST)
    public ResultConstant list(HttpServletRequest request, @RequestBody GradeQO gradeQO) throws Exception {
//    public ResultConstant list(HttpServletRequest request, LXClassQO lxClassQO) throws Exception {

        ValidDTO validDTO = gradeQO.validPageParams(gradeQO);
        if (!validDTO.getResult()) {
            return ResultConstant.illegalParams(validDTO.getMsg());
        }
        try {
            GradeDTO gradeDTO= new GradeDTO();
            int count = gradeService.num(gradeQO);
            if (count > 0) {
                List<Grade> list = gradeService.list(gradeQO);
                gradeDTO.setList(list);
            }
            return ResultConstant.success(gradeDTO);
        } catch (Exception e) {
            e.printStackTrace();

            logger.error("list->error"+e.getMessage());
            return ResultConstant.error("查询失败");
        }
    }

    @Login
    @RequestMapping(value = "/detail", method = RequestMethod.POST)
//    public ResultConstant detail(HttpServletRequest request, @RequestBody LXClass lxClass) {
    public ResultConstant detail(HttpServletRequest request, @RequestBody Grade grade) {
        if (StringUtils.isBlank(grade.getId())) {
            ResultConstant.illegalParams("id不能为空");
        }
        try {
            Grade temp = gradeService.getGradeById(grade.getId());
            if(temp!=null){
                return ResultConstant.success(temp);
            }else {
                return ResultConstant.error("当前成绩不存在");
            }
        }catch (Exception e) {
            e.printStackTrace();
            logger.error("detail->error"+e.getMessage());
            return ResultConstant.error("查看详情失败");
        }
    }
    @Login
    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    public ResultConstant delete(HttpServletResponse response, String id) throws Exception {
        if (StringUtils.isBlank(id)) {
            return ResultConstant.illegalParams("id不能为空");
        }
        try {
            Grade temp = gradeService.getGradeById(id);
            if (temp == null) {
                return ResultConstant.error("当前成绩为空");
            }
            ResultConstant ref = gradeService.deleteById(id);
            return ref;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("delete->error"+e.getMessage());
            return ResultConstant.error("删除失败");
        }
    }
}
