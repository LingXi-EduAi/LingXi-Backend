-- LingXi consolidated database setup (MySQL 8+)
-- One-shot script to create schema, constraints, and seed demo data

-- 0) Create database and session settings
CREATE DATABASE IF NOT EXISTS lx CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE lx;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 1) Core tables (from lx.sql)
DROP TABLE IF EXISTS `lx_grade`;
DROP TABLE IF EXISTS `lx_homework`;
DROP TABLE IF EXISTS `lx_conversation`;
DROP TABLE IF EXISTS `lx_document`;
DROP TABLE IF EXISTS `lx_customer`;
DROP TABLE IF EXISTS `lx_class`;
DROP TABLE IF EXISTS `lx_class_grouping`;
DROP TABLE IF EXISTS `lx_token`;

CREATE TABLE `lx_class`  (
  `id` varchar(32) NOT NULL COMMENT 'id',
  `name` varchar(32) NULL DEFAULT NULL COMMENT '班级名称',
  `subject` varchar(255) NULL DEFAULT NULL COMMENT '科目',
  `information` varchar(1024) NULL DEFAULT NULL COMMENT '基本信息',
  `notice` varchar(1024) NULL DEFAULT NULL COMMENT '消息',
  `class_grouping_id` varchar(32) NULL DEFAULT NULL COMMENT '分班模块id',
  `teacher_id` varchar(32) NULL DEFAULT NULL COMMENT '班主任id',
  `create_id` varchar(32) NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT NULL,
  `update_id` varchar(32) NULL DEFAULT NULL,
  `update_time` datetime NULL DEFAULT NULL,
  `state` varchar(1) NULL DEFAULT NULL,
  `version` varchar(255) NULL DEFAULT NULL COMMENT '版本号',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 ROW_FORMAT = Dynamic;

CREATE TABLE `lx_class_grouping`  (
  `id` varchar(32) NOT NULL COMMENT 'id',
  `name` varchar(32) NULL DEFAULT NULL COMMENT '名字',
  `class_rule` varchar(32) NULL DEFAULT NULL COMMENT '分班规则',
  `class_condition` varchar(32) NULL DEFAULT NULL COMMENT '分班条件',
  `volume` int NULL DEFAULT NULL COMMENT '班级容量',
  `create_id` varchar(32) NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT NULL,
  `update_id` varchar(32) NULL DEFAULT NULL,
  `update_time` datetime NULL DEFAULT NULL,
  `state` varchar(1) NULL DEFAULT NULL,
  `version` varchar(255) NULL DEFAULT NULL COMMENT '版本号',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 ROW_FORMAT = Dynamic;

CREATE TABLE `lx_conversation`  (
  `id` varchar(32) NOT NULL,
  `teacher_id` varchar(32) NULL DEFAULT NULL COMMENT '教师id',
  `student_id` varchar(32) NULL DEFAULT NULL COMMENT '学生id',
  `conversation_id` varchar(255) NULL DEFAULT NULL COMMENT '会话id',
  `create_id` varchar(32) NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT NULL,
  `update_id` varchar(32) NULL DEFAULT NULL,
  `update_time` datetime NULL DEFAULT NULL,
  `state` varchar(1) NULL DEFAULT NULL,
  `version` varchar(255) NULL DEFAULT NULL COMMENT '版本号',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 ROW_FORMAT = Dynamic;

CREATE TABLE `lx_customer`  (
  `id` varchar(32) NOT NULL,
  `user_id` varchar(32) NULL DEFAULT NULL COMMENT '账号',
  `name` varchar(32) NULL DEFAULT NULL COMMENT '账户名',
  `password` varchar(50) NULL DEFAULT NULL COMMENT '密码',
  `class_id` varchar(1024) NULL DEFAULT NULL COMMENT '班级id',
  `grade` int NULL DEFAULT NULL COMMENT '成绩',
  `age` int NULL DEFAULT NULL COMMENT '年龄',
  `phone_number` varchar(11) NULL DEFAULT NULL COMMENT '电话',
  `email` varchar(64) NULL DEFAULT NULL COMMENT '邮箱',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `state` char(1) NULL DEFAULT NULL COMMENT '状态',
  `version` int NULL DEFAULT NULL COMMENT '版本',
  `update_id` varchar(32) NULL DEFAULT NULL COMMENT '用户表id',
  `update_time` datetime NULL DEFAULT NULL COMMENT '编辑时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 ROW_FORMAT = Dynamic;

CREATE TABLE `lx_document`  (
  `id` varchar(32) NOT NULL,
  `name` varchar(255) NULL DEFAULT NULL,
  `type` varchar(255) NULL DEFAULT NULL,
  `description` varchar(1024) NULL DEFAULT NULL COMMENT '描述',
  `file_address` varchar(255) NULL DEFAULT NULL COMMENT '地址',
  `create_id` varchar(32) NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT NULL,
  `update_id` varchar(32) NULL DEFAULT NULL,
  `update_time` datetime NULL DEFAULT NULL,
  `state` varchar(1) NULL DEFAULT NULL,
  `version` varchar(255) NULL DEFAULT NULL COMMENT '版本号',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 ROW_FORMAT = Dynamic;

CREATE TABLE `lx_grade`  (
  `id` varchar(32) NOT NULL,
  `grade` double NULL DEFAULT NULL COMMENT '成绩',
  `class_id` varchar(32) NULL DEFAULT NULL COMMENT '班级id',
  `student_id` varchar(32) NULL DEFAULT NULL COMMENT '学生id',
  `week` varchar(1) NULL DEFAULT NULL COMMENT '周',
  `unit` varchar(1) NULL DEFAULT NULL COMMENT '单元',
  `evaluate` varchar(1) NULL DEFAULT NULL COMMENT '评价0-优秀1-良好2-及格3-不及格',
  `subject` varchar(255) NULL DEFAULT NULL COMMENT '科目',
  `create_id` varchar(32) NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT NULL,
  `update_id` varchar(32) NULL DEFAULT NULL,
  `update_time` datetime NULL DEFAULT NULL,
  `state` varchar(1) NULL DEFAULT NULL,
  `version` varchar(255) NULL DEFAULT NULL COMMENT '版本号',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 ROW_FORMAT = Dynamic;

CREATE TABLE `lx_homework`  (
  `id` varchar(32) NOT NULL,
  `name` varchar(255) NULL DEFAULT NULL COMMENT '标题',
  `content` varchar(255) NULL DEFAULT NULL COMMENT '内容',
  `file_address` varchar(1024) NULL DEFAULT NULL COMMENT '文件地址',
  `student_id` varchar(32) NULL DEFAULT NULL COMMENT '学生id',
  `create_id` varchar(32) NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT NULL,
  `update_id` varchar(32) NULL DEFAULT NULL,
  `update_time` datetime NULL DEFAULT NULL,
  `state` varchar(1) NULL DEFAULT NULL,
  `version` varchar(255) NULL DEFAULT NULL COMMENT '版本号',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 ROW_FORMAT = Dynamic;

CREATE TABLE `lx_token`  (
  `id` varchar(32) NOT NULL COMMENT 'id',
  `user_id` varchar(32) NULL DEFAULT NULL COMMENT '账户',
  `name` varchar(32) NULL DEFAULT NULL COMMENT '用户名',
  `token` varchar(32) NULL DEFAULT NULL COMMENT 'token',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `ip` varchar(255) NULL DEFAULT NULL COMMENT 'ip',
  `state` varchar(1) NULL DEFAULT NULL COMMENT '状态',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 ROW_FORMAT = Dynamic;

-- 2) Study group tables
DROP TABLE IF EXISTS `lx_study_group_message`;
DROP TABLE IF EXISTS `lx_study_group_member`;
DROP TABLE IF EXISTS `lx_study_group`;

CREATE TABLE IF NOT EXISTS lx_study_group (
  id            VARCHAR(64)  PRIMARY KEY,
  name          VARCHAR(64)  NOT NULL,
  description   VARCHAR(512) NULL,
  category      VARCHAR(32)  NULL,
  max_members   INT          DEFAULT 20,
  create_id     VARCHAR(64)  NOT NULL,
  create_time   TIMESTAMP    NOT NULL,
  update_id     VARCHAR(64)  NULL,
  update_time   TIMESTAMP    NULL,
  state         VARCHAR(8)   NOT NULL DEFAULT '1',
  version       INT          NOT NULL DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS lx_study_group_member (
  id          VARCHAR(64) PRIMARY KEY,
  group_id    VARCHAR(64) NOT NULL,
  customer_id VARCHAR(64) NOT NULL,
  role        VARCHAR(16) NOT NULL DEFAULT 'member',
  join_time   TIMESTAMP   NOT NULL,
  state       VARCHAR(8)  NOT NULL DEFAULT '1',
  version     INT         NOT NULL DEFAULT 1,
  UNIQUE KEY uk_group_member (group_id, customer_id),
  KEY idx_group_id (group_id),
  KEY idx_customer_id (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS lx_study_group_message (
  id         VARCHAR(64) PRIMARY KEY,
  group_id   VARCHAR(64) NOT NULL,
  sender_id  VARCHAR(64) NOT NULL,
  content    VARCHAR(2000) NOT NULL,
  create_time TIMESTAMP NOT NULL,
  state      VARCHAR(8)  NOT NULL DEFAULT '1',
  version    INT         NOT NULL DEFAULT 1,
  KEY idx_group_time (group_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3) Indexes and foreign keys
-- base indexes
CREATE INDEX IF NOT EXISTS idx_customer_user_id ON lx_customer(user_id);
CREATE INDEX IF NOT EXISTS idx_customer_email ON lx_customer(email);
CREATE INDEX IF NOT EXISTS idx_customer_phone ON lx_customer(phone_number);
CREATE INDEX IF NOT EXISTS idx_customer_class_id ON lx_customer(class_id);

CREATE INDEX IF NOT EXISTS idx_class_grouping_id ON lx_class(class_grouping_id);
CREATE INDEX IF NOT EXISTS idx_class_teacher_id ON lx_class(teacher_id);

CREATE INDEX IF NOT EXISTS idx_homework_student_id ON lx_homework(student_id);
CREATE INDEX IF NOT EXISTS idx_homework_create_id ON lx_homework(create_id);

CREATE INDEX IF NOT EXISTS idx_document_create_id ON lx_document(create_id);

CREATE INDEX IF NOT EXISTS idx_conv_teacher_id ON lx_conversation(teacher_id);
CREATE INDEX IF NOT EXISTS idx_conv_student_id ON lx_conversation(student_id);

CREATE INDEX IF NOT EXISTS idx_grade_class_id ON lx_grade(class_id);
CREATE INDEX IF NOT EXISTS idx_grade_student_id ON lx_grade(student_id);
CREATE INDEX IF NOT EXISTS idx_grade_create_id ON lx_grade(create_id);

-- foreign keys
ALTER TABLE lx_class
  ADD CONSTRAINT fk_class_grouping
  FOREIGN KEY (class_grouping_id) REFERENCES lx_class_grouping(id)
  ON UPDATE CASCADE ON DELETE SET NULL;

ALTER TABLE lx_class
  ADD CONSTRAINT fk_class_teacher
  FOREIGN KEY (teacher_id) REFERENCES lx_customer(id)
  ON UPDATE CASCADE ON DELETE SET NULL;

ALTER TABLE lx_customer
  ADD CONSTRAINT fk_customer_class
  FOREIGN KEY (class_id) REFERENCES lx_class(id)
  ON UPDATE CASCADE ON DELETE SET NULL;

ALTER TABLE lx_homework
  ADD CONSTRAINT fk_homework_student
  FOREIGN KEY (student_id) REFERENCES lx_customer(id)
  ON UPDATE CASCADE ON DELETE CASCADE,
  ADD CONSTRAINT fk_homework_creator
  FOREIGN KEY (create_id) REFERENCES lx_customer(id)
  ON UPDATE CASCADE ON DELETE SET NULL;

ALTER TABLE lx_document
  ADD CONSTRAINT fk_document_creator
  FOREIGN KEY (create_id) REFERENCES lx_customer(id)
  ON UPDATE CASCADE ON DELETE SET NULL;

ALTER TABLE lx_conversation
  ADD CONSTRAINT fk_conv_teacher
  FOREIGN KEY (teacher_id) REFERENCES lx_customer(id)
  ON UPDATE CASCADE ON DELETE CASCADE,
  ADD CONSTRAINT fk_conv_student
  FOREIGN KEY (student_id) REFERENCES lx_customer(id)
  ON UPDATE CASCADE ON DELETE CASCADE;

ALTER TABLE lx_grade
  ADD CONSTRAINT fk_grade_class
  FOREIGN KEY (class_id) REFERENCES lx_class(id)
  ON UPDATE CASCADE ON DELETE CASCADE,
  ADD CONSTRAINT fk_grade_student
  FOREIGN KEY (student_id) REFERENCES lx_customer(id)
  ON UPDATE CASCADE ON DELETE CASCADE,
  ADD CONSTRAINT fk_grade_creator
  FOREIGN KEY (create_id) REFERENCES lx_customer(id)
  ON UPDATE CASCADE ON DELETE SET NULL;

-- 4) Seed data (minimal demo)
START TRANSACTION;
SET @ts = NOW();
SET @teacher01 = '11111111111111111111111111111111';
SET @teacher02 = '22222222222222222222222222222222';
SET @groupA   = '33333333333333333333333333333333';
SET @groupB   = '44444444444444444444444444444444';
SET @classA   = '55555555555555555555555555555555';
SET @classB   = '66666666666666666666666666666666';
SET @stu01    = '77777777777777777777777777777777';
SET @stu02    = '88888888888888888888888888888888';
SET @doc01    = '99999999999999999999999999999999';
SET @doc02    = 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa';
SET @conv01   = 'bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb';
SET @conv02   = 'cccccccccccccccccccccccccccccccc';
SET @hw01     = 'dddddddddddddddddddddddddddddddd';
SET @hw02     = 'eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee';
SET @grade01  = 'ffffffffffffffffffffffffffffffff';
SET @grade02  = '12121212121212121212121212121212';

INSERT INTO lx_customer (id, user_id, name, password, class_id, grade, age, phone_number, email, create_time, state, version) VALUES
(@teacher01,'teacher01','张老师','e10adc3949ba59abbe56e057f20f883e',NULL,NULL,35,'13900000001','teacher01@example.com',@ts,'1',1),
(@teacher02,'teacher02','李老师','e10adc3949ba59abbe56e057f20f883e',NULL,NULL,33,'13900000002','teacher02@example.com',@ts,'1',1);

INSERT INTO lx_class_grouping (id, name, class_rule, class_condition, volume, create_id, create_time, state, version) VALUES
(@groupA,'默认分组A','size:30','random',30,@teacher01,@ts,'1',1),
(@groupB,'默认分组B','size:40','byGrade',40,@teacher02,@ts,'1',1);

INSERT INTO lx_class (id, name, subject, information, notice, class_grouping_id, teacher_id, create_id, create_time, state, version) VALUES
(@classA,'一年级一班','语文','语文基础班','按时完成作业',@groupA,@teacher01,@teacher01,@ts,'1',1),
(@classB,'二年级一班','数学','数学提高班','上课不许迟到',@groupB,@teacher02,@teacher02,@ts,'1',1);

INSERT INTO lx_customer (id, user_id, name, password, class_id, grade, age, phone_number, email, create_time, state, version) VALUES
(@stu01,'student01','王小明','e10adc3949ba59abbe56e057f20f883e',@classA,85,15,'13900000011','student01@example.com',@ts,'2',1),
(@stu02,'student02','赵小红','e10adc3949ba59abbe56e057f20f883e',@classB,88,16,'13900000012','student02@example.com',@ts,'2',1);

INSERT INTO lx_document (id, name, type, description, file_address, create_id, create_time, state, version) VALUES
(@doc01,'课程大纲','pdf','一年级语文课程大纲','/uploads/syllabus_yg1.pdf',@teacher01,@ts,'1',1),
(@doc02,'习题册','pdf','二年级数学练习册','/uploads/exercise_m2.pdf',@teacher02,@ts,'1',1);

INSERT INTO lx_conversation (id, teacher_id, student_id, conversation_id, create_id, create_time, state, version) VALUES
(@conv01,@teacher01,@stu01,'conv-001',@teacher01,@ts,'1',1),
(@conv02,@teacher02,@stu02,'conv-002',@teacher02,@ts,'1',1);

INSERT INTO lx_homework (id, name, content, file_address, student_id, create_id, create_time, state, version) VALUES
(@hw01,'第1周作业','预习第一单元并完成练习题','/uploads/hw_w1.pdf',@stu01,@teacher01,@ts,'1',1),
(@hw02,'第1周作业','复习上节课内容并完成练习题','/uploads/hw_m1.pdf',@stu02,@teacher02,@ts,'1',1);

INSERT INTO lx_grade (id, grade, class_id, student_id, week, unit, evaluate, subject, create_id, create_time, state, version) VALUES
(@grade01,95,@classA,@stu01,'1','1','A','语文',@teacher01,@ts,'1',1),
(@grade02,92,@classB,@stu02,'1','1','A-','数学',@teacher02,@ts,'1',1);

-- study group seed
SET @owner1 = @teacher01;
SET @owner2 = @teacher02;
SET @g1 = 'g11111111111111111111111111111111';
SET @g2 = 'g22222222222222222222222222222222';

INSERT INTO lx_study_group (id, name, description, category, max_members, create_id, create_time, state, version) VALUES
(@g1,'三年一班英语学习小组','每晚练习口语与听力，周末分享英语演讲','英语',25,@owner1,@ts,'1',1),
(@g2,'三年三班数学学习小组','每日一道难题解析，互相讲题','数学',30,@owner2,@ts,'1',1);

INSERT INTO lx_study_group_member (id, group_id, customer_id, role, join_time, state, version) VALUES
('gm1',@g1,@owner1,'owner',@ts,'1',1),
('gm2',@g2,@owner2,'owner',@ts,'1',1),
('gm3',@g1,@stu01,'member',@ts,'1',1),
('gm4',@g2,@stu02,'member',@ts,'1',1);

INSERT INTO lx_study_group_message (id, group_id, sender_id, content, create_time, state, version) VALUES
('msg1',@g1,@owner1,'欢迎加入英语学习小组！',@ts,'1',1),
('msg2',@g1,@stu01,'大家好，我是王小明',DATE_ADD(@ts, INTERVAL 1 MINUTE),'1',1),
('msg3',@g2,@owner2,'今天讨论二次函数应用题',@ts,'1',1);

COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
