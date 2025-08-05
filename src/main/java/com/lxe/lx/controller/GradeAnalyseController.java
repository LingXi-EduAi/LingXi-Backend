package com.lxe.lx.controller;

import com.lxe.lx.annotation.Login;
import com.lxe.lx.domain.qo.CustomerQO;
import com.lxe.lx.domain.qo.GradeQO;
import com.lxe.lx.pojo.Customer;
import com.lxe.lx.pojo.Grade;
import com.lxe.lx.pojo.GradeAnalyse;
import com.lxe.lx.pojo.TokenEntity;
import com.lxe.lx.service.CustomerService;
import com.lxe.lx.service.GradeService;
import com.lxe.lx.util.ResultConstant;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static com.lxe.lx.config.AuthorizationInterceptor.ORG_ID_KEY;
@RestController
@RequestMapping("/gradeAnalyse")
public class GradeAnalyseController {
    Logger logger = LogManager.getLogger(GradeController.class);
    @Autowired
    private GradeService gradeService;
    @Autowired
    private CustomerService customerService;
    @Login
    @RequestMapping(value = "/overviewTeacher", method = RequestMethod.POST)
    public ResultConstant overviewTeacher(HttpServletRequest request, @RequestBody GradeAnalyse gradeAnalyse) {
        if (gradeAnalyse == null || StringUtils.isBlank(gradeAnalyse.getClassId())) {
            return ResultConstant.illegalParams("班级不能为空");
        }else if (StringUtils.isBlank(gradeAnalyse.getWeek())) {
            return ResultConstant.illegalParams("week不能为空");
        }else if (StringUtils.isBlank(gradeAnalyse.getUnit())) {
            return ResultConstant.illegalParams("单元不能为空");
        }
        try{
            TokenEntity tokenEntity = (TokenEntity) request.getAttribute(ORG_ID_KEY);
            Customer temp = customerService.getCustomerById(tokenEntity.getId());
            if(!"1".equals(temp.getState())){
                return ResultConstant.notAuthorized();
            }
            CustomerQO customerQOTemp = new CustomerQO();
            customerQOTemp.setClassId(gradeAnalyse.getClassId());
            Integer studentNum = customerService.num(customerQOTemp);
            gradeAnalyse.setStudentNum(studentNum);
            GradeQO gradeQOTemp = new GradeQO();
            gradeQOTemp.setClassId(gradeAnalyse.getClassId());
            gradeQOTemp.setWeek(gradeAnalyse.getWeek());
            gradeQOTemp.setUnit(gradeAnalyse.getUnit());
            Integer finishNum = gradeService.num(gradeQOTemp);
            List<Grade> gradeListTemp = gradeService.list(gradeQOTemp);
            double sum=0.0;
            if (finishNum != null && studentNum != null && studentNum != 0) {
                double progressPercentage = (studentNum == 0) ? 0.0 :
                        new BigDecimal((double)finishNum / studentNum)
                                .setScale(2, RoundingMode.HALF_UP)
                                .doubleValue();
                gradeAnalyse.setProgressPercentage(progressPercentage);
            }else {
                return ResultConstant.error("学生数为零");
            }
            for (Grade grade : gradeListTemp) {
                sum += grade.getGrade();  // grade 是 double 类型
            }
            double averageGrade = (finishNum == 0) ? 0.0 : BigDecimal.valueOf(sum / finishNum).divide(BigDecimal.valueOf(finishNum), 2, RoundingMode.HALF_UP)
                    .doubleValue();
            gradeAnalyse.setAverageScore(averageGrade);
            return ResultConstant.success(gradeAnalyse);
        }catch (Exception e){
            e.printStackTrace();
            logger.error("overviewTeacher->error"+e.getMessage());

            return ResultConstant.error("查询失败");
        }
    }
    @Login
    @RequestMapping(value = "/gradeDistributionTeacher", method = RequestMethod.POST)
    public ResultConstant gradeDistributionTeacher(HttpServletRequest request, @RequestBody GradeAnalyse gradeAnalyse) {
        if (gradeAnalyse == null || StringUtils.isBlank(gradeAnalyse.getClassId())) {
            return ResultConstant.illegalParams("班级不能为空");
        }
        try {
            TokenEntity tokenEntity = (TokenEntity) request.getAttribute(ORG_ID_KEY);
            Customer temp = customerService.getCustomerById(tokenEntity.getId());
            if (!"1".equals(temp.getState())) {
                return ResultConstant.notAuthorized();
            }
            GradeQO gradeQOTemp = new GradeQO();
            gradeQOTemp.setClassId(gradeAnalyse.getClassId());
            gradeQOTemp.setEvaluate("0");
            Integer numTemp0 = gradeService.num(gradeQOTemp);
            Double numDouble0 = (numTemp0 == null) ? 0.0 : numTemp0.doubleValue();
            gradeQOTemp.setEvaluate("1");
            Integer numTemp1 = gradeService.num(gradeQOTemp);
            Double numDouble1 = (numTemp0 == null) ? 0.0 : numTemp1.doubleValue();
            gradeQOTemp.setEvaluate("2");
            Integer numTemp2 = gradeService.num(gradeQOTemp);
            Double numDouble2 = (numTemp0 == null) ? 0.0 : numTemp2.doubleValue();
            gradeQOTemp.setEvaluate("3");
            Integer numTemp3 = gradeService.num(gradeQOTemp);
            Double numDouble3 = (numTemp0 == null) ? 0.0 : numTemp3.doubleValue();
            gradeAnalyse.getDataList().add(numDouble0);
            gradeAnalyse.getDataList().add(numDouble1);
            gradeAnalyse.getDataList().add(numDouble2);
            gradeAnalyse.getDataList().add(numDouble3);
            return ResultConstant.success(gradeAnalyse);
        }catch (Exception e){
            e.printStackTrace();
            logger.error("gradeDistributionTeacher->error"+e.getMessage());

            return ResultConstant.error("查询失败");
        }
    }
    @Login
    @RequestMapping(value = "/unitProgressTeacher", method = RequestMethod.POST)
    public ResultConstant unitProgressTeacher(HttpServletRequest request, @RequestBody GradeAnalyse gradeAnalyse) {
        if (gradeAnalyse == null || StringUtils.isBlank(gradeAnalyse.getClassId())) {
            return ResultConstant.illegalParams("班级不能为空");
        }
        try{
            TokenEntity tokenEntity = (TokenEntity) request.getAttribute(ORG_ID_KEY);
            Customer temp = customerService.getCustomerById(tokenEntity.getId());
            if (!"1".equals(temp.getState())) {
                return ResultConstant.notAuthorized();
            }
            CustomerQO customerQOTemp = new CustomerQO();
            customerQOTemp.setClassId(gradeAnalyse.getClassId());
            Integer studentNum = customerService.num(customerQOTemp);
            GradeQO gradeQOTemp = new GradeQO();
            gradeQOTemp.setClassId(gradeAnalyse.getClassId());
            String limit = gradeService.maxUnit(gradeQOTemp);
            int limitInt = (limit == null || limit.isEmpty()) ? 0 : Integer.parseInt(limit);
            for(int i=0;i<limitInt;i++){
                GradeQO gradeQOTempCycle = new GradeQO();
                gradeQOTempCycle.setClassId(gradeAnalyse.getClassId());
                String StrI = String.valueOf(i+1);
                gradeQOTempCycle.setUnit(StrI);
                Integer numTemp1 = gradeService.num(gradeQOTempCycle);
                Double result1 = (studentNum == null || studentNum == 0 || numTemp1 == null) ? 0.0 :
                        new BigDecimal((double) numTemp1 / studentNum)
                                .setScale(2, RoundingMode.HALF_UP)
                                .doubleValue();
                gradeAnalyse.getDataList().add(result1);
            }
            return ResultConstant.success(gradeAnalyse);
        }catch (Exception e){
            e.printStackTrace();
            logger.error("unitProgressTeacher->error"+e.getMessage());

            return ResultConstant.error("查询失败");
        }
    }
    @Login
    @RequestMapping(value = "/learningTrendTeacher", method = RequestMethod.POST)
    public ResultConstant learningTrendTeacher(HttpServletRequest request, @RequestBody GradeAnalyse gradeAnalyse) {
        if (gradeAnalyse == null || StringUtils.isBlank(gradeAnalyse.getClassId())) {
            return ResultConstant.illegalParams("班级不能为空");
        }
        try{
            TokenEntity tokenEntity = (TokenEntity) request.getAttribute(ORG_ID_KEY);
            Customer temp = customerService.getCustomerById(tokenEntity.getId());
            if (!"1".equals(temp.getState())) {
                return ResultConstant.notAuthorized();
            }
            CustomerQO customerQOTemp = new CustomerQO();
            customerQOTemp.setClassId(gradeAnalyse.getClassId());
            Integer studentNum = customerService.num(customerQOTemp);
            GradeQO gradeQOTemp = new GradeQO();
            gradeQOTemp.setClassId(gradeAnalyse.getClassId());
            String limit = gradeService.maxWeek(gradeQOTemp);
            int limitInt = (limit == null || limit.isEmpty()) ? 0 : Integer.parseInt(limit);
            for(int i=0;i<limitInt;i++){
                GradeQO gradeQOTempCycle = new GradeQO();
                gradeQOTempCycle.setClassId(gradeAnalyse.getClassId());
                String StrI = String.valueOf(i+1);
                gradeQOTempCycle.setWeek(StrI);
                Integer numTemp1 = gradeService.num(gradeQOTempCycle);
                Double result1 = (studentNum == null || studentNum == 0 || numTemp1 == null) ? 0.0 :
                        new BigDecimal((double) numTemp1 / studentNum)
                                .setScale(2, RoundingMode.HALF_UP)
                                .doubleValue();
                gradeAnalyse.getDataList().add(result1);
            }
            return ResultConstant.success(gradeAnalyse);
        }catch (Exception e){
            e.printStackTrace();
            logger.error("learningTrendTeacher->error"+e.getMessage());

            return ResultConstant.error("查询失败");
        }
    }



    @Login
    @RequestMapping(value = "/overviewStudent", method = RequestMethod.POST)
    public ResultConstant overviewStudent(HttpServletRequest request, @RequestBody GradeQO gradeQO) {
        if (gradeQO == null || StringUtils.isBlank(gradeQO.getStudentId())) {
            return ResultConstant.illegalParams("学生ID不能为空");
        }
        try{
            Integer num = gradeService.num(gradeQO);
            List<Grade> list = gradeService.list(gradeQO);
            double sum =0.0;
            for(Grade temp:list){
                sum = sum+temp.getGrade();
            }
            double averageGrade = (num == 0) ? 0.0 : BigDecimal.valueOf(sum / num).divide(BigDecimal.valueOf(num), 2, RoundingMode.HALF_UP)
                    .doubleValue();
            GradeAnalyse gradeAnalyse = new GradeAnalyse();
            gradeAnalyse.setAverageScore(averageGrade);
            return ResultConstant.success(gradeAnalyse);
        }catch (Exception e){
            e.printStackTrace();
            logger.error("overviewStudent->error"+e.getMessage());

            return ResultConstant.error("查询失败");
        }
    }
    @Login
    @RequestMapping(value = "/gradeDistributionStudent", method = RequestMethod.POST)
    public ResultConstant gradeDistributionStudent(HttpServletRequest request, @RequestBody GradeQO gradeQO) {
        if (gradeQO == null || StringUtils.isBlank(gradeQO.getStudentId())) {
            return ResultConstant.illegalParams("学生ID不能为空");
        }
        try {
            gradeQO.setEvaluate("0");
            Integer numTemp0 = gradeService.num(gradeQO);
            Double numDouble0 = (numTemp0 == null) ? 0.0 : numTemp0.doubleValue();
            gradeQO.setEvaluate("1");
            Integer numTemp1 = gradeService.num(gradeQO);
            Double numDouble1 = (numTemp0 == null) ? 0.0 : numTemp1.doubleValue();
            gradeQO.setEvaluate("2");
            Integer numTemp2 = gradeService.num(gradeQO);
            Double numDouble2 = (numTemp0 == null) ? 0.0 : numTemp2.doubleValue();
            gradeQO.setEvaluate("3");
            Integer numTemp3 = gradeService.num(gradeQO);
            Double numDouble3 = (numTemp0 == null) ? 0.0 : numTemp3.doubleValue();
            GradeAnalyse gradeAnalyse = new GradeAnalyse();
            gradeAnalyse.getDataList().add(numDouble0);
            gradeAnalyse.getDataList().add(numDouble1);
            gradeAnalyse.getDataList().add(numDouble2);
            gradeAnalyse.getDataList().add(numDouble3);
            return ResultConstant.success(gradeAnalyse);
        }catch (Exception e){
            e.printStackTrace();
            logger.error("gradeDistributionsStudent->error"+e.getMessage());

            return ResultConstant.error("查询失败");
        }
    }
    @Login
    @RequestMapping(value = "/unitProgressStudent", method = RequestMethod.POST)
    public ResultConstant unitProgressStudent(HttpServletRequest request, @RequestBody GradeQO gradeQO) {
        if (gradeQO == null || StringUtils.isBlank(gradeQO.getStudentId())) {
            return ResultConstant.illegalParams("学生ID不能为空");
        }else if (StringUtils.isBlank(gradeQO.getClassId())) {
            return ResultConstant.illegalParams("班级不能为空");
        }
        try{
            GradeQO gradeQOTemp = new GradeQO();
            gradeQOTemp.setClassId(gradeQO.getClassId());
            String limit = gradeService.maxUnit(gradeQOTemp);
            int limitInt = (limit == null || limit.isEmpty()) ? 0 : Integer.parseInt(limit);
            GradeAnalyse gradeAnalyse = new GradeAnalyse();
            for(int i=0;i<limitInt;i++){
                GradeQO gradeQOTempCycle = new GradeQO();
                gradeQOTempCycle.setStudentId(gradeQO.getStudentId());
                gradeQOTempCycle.setClassId(gradeQO.getClassId());
                String StrI = String.valueOf(i+1);
                gradeQOTempCycle.setUnit(StrI);
                Integer numTemp1 = gradeService.num(gradeQOTempCycle);
                Double result1 = (numTemp1 == null) ? 0.0 : numTemp1.doubleValue();
                gradeAnalyse.getDataList().add(result1);
            }
            return ResultConstant.success(gradeAnalyse);
        }catch (Exception e){
            e.printStackTrace();
            logger.error("unitProgressStudent->error"+e.getMessage());

            return ResultConstant.error("查询失败");
        }
    }
    @Login
    @RequestMapping(value = "/learningTrendStudent", method = RequestMethod.POST)
    public ResultConstant learningTrendStudent(HttpServletRequest request, @RequestBody GradeQO gradeQO) {
        if (gradeQO == null || StringUtils.isBlank(gradeQO.getStudentId())) {
            return ResultConstant.illegalParams("学生ID不能为空");
        }else if (StringUtils.isBlank(gradeQO.getClassId())) {
            return ResultConstant.illegalParams("班级不能为空");
        }
        try{
            GradeQO gradeQOTemp = new GradeQO();
            gradeQOTemp.setClassId(gradeQO.getClassId());
            String limit = gradeService.maxWeek(gradeQOTemp);
            int limitInt = (limit == null || limit.isEmpty()) ? 0 : Integer.parseInt(limit);
            GradeAnalyse gradeAnalyse = new GradeAnalyse();
            for(int i=0;i<limitInt;i++){
                GradeQO gradeQOTempCycle = new GradeQO();
                gradeQOTempCycle.setStudentId(gradeQO.getStudentId());
                gradeQOTempCycle.setClassId(gradeQO.getClassId());
                String StrI = String.valueOf(i+1);
                gradeQOTempCycle.setWeek(StrI);
                Integer numTemp1 = gradeService.num(gradeQOTempCycle);
                Double result1 = (numTemp1 == null) ? 0.0 : numTemp1.doubleValue();
                gradeAnalyse.getDataList().add(result1);
            }
            return ResultConstant.success(gradeAnalyse);
        }catch (Exception e){
            e.printStackTrace();
            logger.error("unitProgressStudent->error"+e.getMessage());

            return ResultConstant.error("查询失败");
        }
    }
}
