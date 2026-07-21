package com.lxe.lx.controller;

import com.lxe.lx.annotation.Login;
import com.lxe.lx.annotation.TeacherOnly;
import com.lxe.lx.util.ResultConstant;
import com.lxe.lx.pojo.HomeworkSubmission;
import com.lxe.lx.pojo.HomeworkAssignment;
import com.lxe.lx.domain.qo.HomeworkSubmissionQO;
import com.lxe.lx.service.HomeworkSubmissionService;
import com.lxe.lx.service.HomeworkAssignmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 学情分析控制器
 */
@RestController
@RequestMapping("/analytics")
@TeacherOnly
public class AnalyticsController {

    @Autowired
    private HomeworkSubmissionService homeworkSubmissionService;
    
    @Autowired
    private HomeworkAssignmentService homeworkAssignmentService;

    /**
     * 获取学情分析统计数据
     */
    @Login
    @PostMapping("/stats")
    public Map<String, Object> getAnalyticsStats(HttpServletRequest request) {
        try {
            System.out.println("========== 开始获取学情分析数据 ==========");
            
            // 获取所有作业提交
            HomeworkSubmissionQO qo = new HomeworkSubmissionQO();
            System.out.println("创建查询对象: " + qo);
            
            List<HomeworkSubmission> submissions = homeworkSubmissionService.list(qo);
            System.out.println("查询到的作业提交数量: " + (submissions != null ? submissions.size() : "null"));
            
            if (submissions == null) {
                System.out.println("警告：submissions为null，创建空列表");
                submissions = new ArrayList<>();
            }
            
            // 统计数据
            int totalSubmissions = submissions.size();
            System.out.println("总提交数: " + totalSubmissions);
            int gradedCount = 0;
            List<Integer> grades = new ArrayList<>();
            
            // 成绩分布统计
            int excellent = 0;  // 90-100
            int good = 0;       // 80-89
            int pass = 0;       // 60-79
            int fail = 0;       // <60
            
            for (HomeworkSubmission submission : submissions) {
                String status = submission.getStatus();
                Integer grade = submission.getGrade();
                String studentName = submission.getStudentName();
                
                System.out.println("处理作业 - 学生: " + studentName + ", 状态: " + status + ", 分数: " + grade);
                
                if ("graded".equals(status) && grade != null) {
                    gradedCount++;
                    grades.add(grade);
                    
                    // 统计成绩分布
                    if (grade >= 90) {
                        excellent++;
                    } else if (grade >= 80) {
                        good++;
                    } else if (grade >= 60) {
                        pass++;
                    } else {
                        fail++;
                    }
                }
            }
            
            System.out.println("已批改数量: " + gradedCount);
            System.out.println("成绩分布 - 优秀: " + excellent + ", 良好: " + good + ", 及格: " + pass + ", 不及格: " + fail);
            
            // 计算完成率
            double completionRate = totalSubmissions > 0 ? (double) gradedCount / totalSubmissions * 100 : 0;
            
            // 计算平均分
            double avgScore = 0;
            if (!grades.isEmpty()) {
                int sum = 0;
                for (int grade : grades) {
                    sum += grade;
                }
                avgScore = (double) sum / grades.size();
            }
            
            // 构建返回数据
            Map<String, Object> result = new HashMap<>();
            Map<String, Object> data = new HashMap<>();
            
            // 基础统计
            data.put("completionRate", Math.round(completionRate));
            data.put("averageScore", Math.round(avgScore * 10) / 10.0);
            data.put("totalSubmissions", totalSubmissions);
            data.put("gradedCount", gradedCount);
            
            // 成绩分布
            Map<String, Object> gradeDistribution = new HashMap<>();
            gradeDistribution.put("labels", Arrays.asList("优秀(90-100)", "良好(80-89)", "及格(60-79)", "不及格(<60)"));
            gradeDistribution.put("data", Arrays.asList(excellent, good, pass, fail));
            data.put("gradeDistribution", gradeDistribution);
            
            // 优秀学生（取前5名）
            List<Map<String, Object>> topStudents = getTopStudents(submissions);
            data.put("topStudents", topStudents);
            
            // 需要关注的学生（分数低于70分的）
            List<Map<String, Object>> studentsNeedAttention = getStudentsNeedAttention(submissions);
            data.put("studentsNeedAttention", studentsNeedAttention);
            
            // 薄弱知识点分析（正确率<70%的作业）
            List<Map<String, Object>> weakPoints = getWeakPoints(submissions);
            data.put("weakPoints", weakPoints);
            data.put("weakPointsCount", weakPoints.size());
            
            result.put("status", ResultConstant.SUCCESS);
            result.put("msg", "获取学情分析数据成功");
            result.put("data", data);
            
            System.out.println("========== 返回数据 ==========");
            System.out.println("完成率: " + data.get("completionRate"));
            System.out.println("平均分: " + data.get("averageScore"));
            System.out.println("总提交数: " + data.get("totalSubmissions"));
            System.out.println("已批改数: " + data.get("gradedCount"));
            System.out.println("========== 学情分析数据返回完成 ==========");
            
            return result;
            
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> result = new HashMap<>();
            result.put("status", ResultConstant.ERROR);
            result.put("msg", "获取学情分析数据失败：" + e.getMessage());
            return result;
        }
    }
    
    /**
     * 获取优秀学生列表
     */
    private List<Map<String, Object>> getTopStudents(List<HomeworkSubmission> submissions) {
        // 按学生分组统计
        Map<String, StudentStats> studentStatsMap = new HashMap<>();
        
        for (HomeworkSubmission submission : submissions) {
            String status = submission.getStatus();
            Integer grade = submission.getGrade();
            
            if ("graded".equals(status) && grade != null) {
                String studentName = submission.getStudentName();
                
                StudentStats stats = studentStatsMap.getOrDefault(studentName, new StudentStats(studentName));
                stats.addGrade(grade);
                studentStatsMap.put(studentName, stats);
            }
        }
        
        // 排序取前5名
        List<StudentStats> statsList = new ArrayList<>(studentStatsMap.values());
        statsList.sort((a, b) -> Double.compare(b.getAvgScore(), a.getAvgScore()));
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < Math.min(5, statsList.size()); i++) {
            StudentStats stats = statsList.get(i);
            if (stats.getAvgScore() >= 85) {  // 优秀学生标准
                Map<String, Object> student = new HashMap<>();
                student.put("name", stats.getName());
                student.put("score", (int) Math.round(stats.getAvgScore()));
                student.put("improvement", "+5%");  // 示例数据
                student.put("strengths", "综合能力强");
                result.add(student);
            }
        }
        
        return result;
    }
    
    /**
     * 获取需要关注的学生列表
     */
    private List<Map<String, Object>> getStudentsNeedAttention(List<HomeworkSubmission> submissions) {
        // 按学生分组统计
        Map<String, StudentStats> studentStatsMap = new HashMap<>();
        
        for (HomeworkSubmission submission : submissions) {
            String status = submission.getStatus();
            Integer grade = submission.getGrade();
            
            if ("graded".equals(status) && grade != null) {
                String studentName = submission.getStudentName();
                
                StudentStats stats = studentStatsMap.getOrDefault(studentName, new StudentStats(studentName));
                stats.addGrade(grade);
                studentStatsMap.put(studentName, stats);
            }
        }
        
        // 筛选需要关注的学生
        List<Map<String, Object>> result = new ArrayList<>();
        for (StudentStats stats : studentStatsMap.values()) {
            if (stats.getAvgScore() < 75) {  // 需要关注的标准
                Map<String, Object> student = new HashMap<>();
                student.put("name", stats.getName());
                student.put("score", (int) Math.round(stats.getAvgScore()));
                
                // 根据分数段给出建议
                if (stats.getAvgScore() < 60) {
                    student.put("weakness", "基础薄弱");
                    student.put("suggestion", "一对一辅导");
                } else if (stats.getAvgScore() < 70) {
                    student.put("weakness", "知识掌握不牢");
                    student.put("suggestion", "加强基础训练");
                } else {
                    student.put("weakness", "部分知识点欠缺");
                    student.put("suggestion", "针对性练习");
                }
                
                result.add(student);
            }
        }
        
        // 按分数从低到高排序
        result.sort((a, b) -> Integer.compare((Integer) a.get("score"), (Integer) b.get("score")));
        
        return result;
    }
    
    /**
     * 获取薄弱知识点列表（平均分低于80分的作业）
     */
    private List<Map<String, Object>> getWeakPoints(List<HomeworkSubmission> submissions) {
        List<Map<String, Object>> weakPoints = new ArrayList<>();
        
        System.out.println("============ 开始分析薄弱知识点 ============");
        System.out.println("总提交数: " + submissions.size());
        
        // 按作业分组，计算每份作业的平均分
        Map<String, List<HomeworkSubmission>> submissionsByAssignment = submissions.stream()
                .filter(s -> "graded".equals(s.getStatus()) && s.getGrade() != null)
                .collect(Collectors.groupingBy(HomeworkSubmission::getAssignmentId));
        
        System.out.println("已批改且有分数的作业数: " + submissionsByAssignment.size());
        
        for (Map.Entry<String, List<HomeworkSubmission>> entry : submissionsByAssignment.entrySet()) {
            String assignmentId = entry.getKey();
            List<HomeworkSubmission> assignmentSubmissions = entry.getValue();
            
            // 计算该作业的平均分
            double avgGrade = assignmentSubmissions.stream()
                    .mapToInt(HomeworkSubmission::getGrade)
                    .average()
                    .orElse(0.0);
            
            // 打印每个作业的详细信息
            System.out.println("---");
            System.out.println("作业ID: " + assignmentId);
            System.out.println("提交人数: " + assignmentSubmissions.size());
            System.out.print("分数列表: ");
            assignmentSubmissions.forEach(s -> System.out.print(s.getGrade() + " "));
            System.out.println();
            System.out.println("平均分: " + avgGrade);
            System.out.println("是否<80: " + (avgGrade < 80));
            
            // 如果平均分低于80分，认为是薄弱知识点
            if (avgGrade < 80) {
                System.out.println("✅ 识别为薄弱知识点（平均分<80）");
                try {
                    // 获取作业详情
                    HomeworkAssignment assignment = homeworkAssignmentService.getById(assignmentId);
                    System.out.println("获取作业详情: " + (assignment != null ? "成功" : "失败(null)"));
                    if (assignment != null) {
                        System.out.println("作业标题: " + assignment.getTitle());
                        System.out.println("作业学科: " + assignment.getSubject());
                        
                        Map<String, Object> weakPoint = new HashMap<>();
                        weakPoint.put("subject", assignment.getSubject());
                        weakPoint.put("title", assignment.getTitle());
                        weakPoint.put("avgScore", Math.round(avgGrade * 10) / 10.0);
                        weakPoint.put("submissionCount", assignmentSubmissions.size());
                        weakPoint.put("assignmentId", assignmentId);
                        
                        // 根据平均分给出建议
                        String suggestion;
                        if (avgGrade < 60) {
                            suggestion = "该知识点掌握极差，建议重新讲解并加强练习";
                        } else if (avgGrade < 70) {
                            suggestion = "该知识点掌握较差，建议复习巩固并针对性训练";
                        } else {
                            suggestion = "该知识点掌握一般，建议强化练习提高熟练度";
                        }
                        weakPoint.put("suggestion", suggestion);
                        
                        weakPoints.add(weakPoint);
                        System.out.println("✅ 已添加到薄弱知识点列表");
                    } else {
                        System.err.println("❌ 无法获取作业详情，跳过此作业");
                    }
                } catch (Exception e) {
                    System.err.println("❌ 获取作业详情失败: assignmentId=" + assignmentId + ", error=" + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.out.println("⭕ 不是薄弱知识点（平均分>=80）");
            }
        }
        
        // 按平均分从低到高排序
        weakPoints.sort((a, b) -> Double.compare(
                (Double) a.get("avgScore"), 
                (Double) b.get("avgScore")
        ));
        
        System.out.println("============ 薄弱知识点分析完成 ============");
        System.out.println("最终识别到的薄弱知识点数量: " + weakPoints.size());
        if (weakPoints.size() > 0) {
            System.out.println("薄弱知识点列表:");
            for (Map<String, Object> wp : weakPoints) {
                System.out.println("  - " + wp.get("subject") + " - " + wp.get("title") + " (平均分: " + wp.get("avgScore") + ")");
            }
        }
        System.out.println("==========================================");
        
        return weakPoints;
    }
    
    /**
     * 内部类：学生统计数据
     */
    private static class StudentStats {
        private String name;
        private List<Integer> grades = new ArrayList<>();
        
        public StudentStats(String name) {
            this.name = name;
        }
        
        public void addGrade(int grade) {
            grades.add(grade);
        }
        
        public String getName() {
            return name;
        }
        
        public double getAvgScore() {
            if (grades.isEmpty()) {
                return 0;
            }
            int sum = 0;
            for (int grade : grades) {
                sum += grade;
            }
            return (double) sum / grades.size();
        }
    }
}

