-- =============================================================================
-- BE-07 演示种子数据 demo_ai_data.sql
-- 用途：让 模型日志页 / 学情页 / 回放 有货可看。
-- 安全：可重复导入（先 DELETE 再 INSERT），多次执行不报错、不重复。
-- 约定：演示用户 id 使用 demo-teacher-0001；会话/任务/事件/证据/日志使用
--       DEMO 前缀的稳定 ID，防止与生产数据冲突。
-- 注意：ai_* 表无外键约束，此处通过一致的 ID 自洽关联即可。
-- =============================================================================

USE lingxi;

-- ---------- 1. 清空演示数据（按依赖倒序） ----------
DELETE FROM ai_model_call_log WHERE task_id LIKE 'DEMO%';
DELETE FROM ai_evidence      WHERE message_id LIKE 'DEMO%';
DELETE FROM ai_message       WHERE task_id LIKE 'DEMO%';
DELETE FROM ai_event         WHERE task_id LIKE 'DEMO%';
DELETE FROM ai_subtask       WHERE task_id LIKE 'DEMO%';
DELETE FROM ai_task          WHERE id LIKE 'DEMO%';
DELETE FROM ai_conversation  WHERE id LIKE 'DEMO%';

-- ---------- 2. 演示会话（3 个） ----------
INSERT INTO ai_conversation
(`id`, `user_id`, `title`, `state`, `version`, `created_at`, `updated_at`) VALUES
('DEMO-CONV-0001', 'demo-teacher-0001', '二次函数最值求解',       'ACTIVE', 1, '2026-09-01 08:00:00.000', '2026-09-01 09:00:00.000'),
('DEMO-CONV-0002', 'demo-teacher-0001', '牛顿第三定律受力分析',   'ACTIVE', 1, '2026-09-01 10:00:00.000', '2026-09-01 11:00:00.000'),
('DEMO-CONV-0003', 'demo-teacher-0001', '化学反应速率实验',       'ACTIVE', 1, '2026-09-02 14:00:00.000', '2026-09-02 15:00:00.000');

-- ---------- 3. AI 任务（6 条：3 SUCCEEDED + 2 FAILED + 1 RUNNING） ----------
INSERT INTO ai_task
(`id`, `user_id`, `conversation_id`, `task_type`, `status`, `progress`,
 `request_json`, `result_json`, `error_code`, `error_message`,
 `dify_task_id`, `dify_conversation_id`, `event_sequence`, `version`,
 `created_at`, `started_at`, `finished_at`, `updated_at`) VALUES
-- 任务 1：成功（数学）
('DEMO-TASK-0001','demo-teacher-0001','DEMO-CONV-0001','WORKFLOW','SUCCEEDED',100,
 '{"query":"帮我求解二次函数 f(x)=x^2-4x+3 的最值并画草图"}',
 '{"answer":"顶点式=(x-2)^2-1，最小值-1，x=2 时取到"}',NULL,NULL,
 'dfy-task-0001','dfy-conv-0001',18,1,
 '2026-09-01 08:00:05.000','2026-09-01 08:00:06.000','2026-09-01 08:01:20.000','2026-09-01 08:01:20.000'),
-- 任务 2：成功（物理）
('DEMO-TASK-0002','demo-teacher-0001','DEMO-CONV-0002','WORKFLOW','SUCCEEDED',100,
 '{"query":"对斜面上的物块做牛顿第二定律受力分析"}',
 '{"answer":"下滑分力 mg·sinθ，支持力 mg·cosθ，摩擦力 μ·mg·cosθ"}',NULL,NULL,
 'dfy-task-0002','dfy-conv-0002',15,1,
 '2026-09-01 10:00:05.000','2026-09-01 10:00:06.000','2026-09-01 10:01:10.000','2026-09-01 10:01:10.000'),
-- 任务 3：成功（化学）
('DEMO-TASK-0003','demo-teacher-0001','DEMO-CONV-0003','WORKFLOW','SUCCEEDED',100,
 '{"query":"分析锌与稀硫酸反应速率的决定因素"}',
 '{"answer":"浓度、温度、接触面积共同决定反应速率"}',NULL,NULL,
 'dfy-task-0003','dfy-conv-0003',20,1,
 '2026-09-02 14:00:05.000','2026-09-02 14:00:06.000','2026-09-02 14:02:05.000','2026-09-02 14:02:05.000'),
-- 任务 4：失败（超时/调用错误）
('DEMO-TASK-0004','demo-teacher-0001','DEMO-CONV-0001','WORKFLOW','FAILED',40,
 '{"query":"求解三重积分并在球坐标系下图示"}',
 NULL,'DIFY_TIMEOUT','Dify 响应超时',NULL,NULL,9,1,
 '2026-09-02 09:00:05.000','2026-09-02 09:00:06.000','2026-09-02 09:00:30.000','2026-09-02 09:00:30.000'),
-- 任务 5：失败（输出解析失败）
('DEMO-TASK-0005','demo-teacher-0001','DEMO-CONV-0002','CHATFLOW','FAILED',20,
 '{"query":"总结狭义相对论的时间膨胀，给出公式推导"}',
 NULL,'PARSE_ERROR','事件解析失败',NULL,NULL,6,1,
 '2026-09-03 16:00:05.000','2026-09-03 16:00:06.000','2026-09-03 16:00:20.000','2026-09-03 16:00:20.000'),
-- 任务 6：运行中（用于展示 RUNNING 状态）
('DEMO-TASK-0006','demo-teacher-0001','DEMO-CONV-0003','WORKFLOW','RUNNING',60,
 '{"query":"设计一套测定中和反应热效应的实验方案"}',
 NULL,NULL,NULL,'dfy-task-0006','dfy-conv-0003',12,1,
 '2026-09-03 20:00:05.000','2026-09-03 20:00:06.000',NULL,'2026-09-03 20:00:40.000');

-- ---------- 4. 子任务（任务 1/2/3 各拆 2 条，共 6 条） ----------
INSERT INTO ai_subtask
(`id`, `task_id`, `parent_id`, `agent_type`, `goal`, `inputs_json`, `dependency_json`,
 `status`, `execution_no`, `retry_count`, `error_code`, `error_message`,
 `version`, `created_at`, `started_at`, `finished_at`, `updated_at`) VALUES
('DEMO-SUB-0001','DEMO-TASK-0001',NULL,'WORKFLOW','将二次函数化为顶点式','{"step":"complete_square"}','[]','SUCCEEDED',1,0,NULL,NULL,1,'2026-09-01 08:00:07.000','2026-09-01 08:00:08.000','2026-09-01 08:00:40.000','2026-09-01 08:00:40.000'),
('DEMO-SUB-0002','DEMO-TASK-0001','DEMO-SUB-0001','WORKFLOW','由顶点式求解最值','{"step":"find_extremum"}','["DEMO-SUB-0001"]','SUCCEEDED',2,0,NULL,NULL,1,'2026-09-01 08:00:41.000','2026-09-01 08:00:42.000','2026-09-01 08:01:15.000','2026-09-01 08:01:15.000'),
('DEMO-SUB-0003','DEMO-TASK-0002',NULL,'WORKFLOW','受力分析并列出平衡方程','{"step":"force_analysis"}','[]','SUCCEEDED',1,0,NULL,NULL,1,'2026-09-01 10:00:07.000','2026-09-01 10:00:08.000','2026-09-01 10:00:55.000','2026-09-01 10:00:55.000'),
('DEMO-SUB-0004','DEMO-TASK-0002','DEMO-SUB-0003','WORKFLOW','用牛顿第二定律推导加速度','{"step":"second_law"}','["DEMO-SUB-0003"]','SUCCEEDED',2,0,NULL,NULL,1,'2026-09-01 10:00:56.000','2026-09-01 10:00:57.000','2026-09-01 10:01:05.000','2026-09-01 10:01:05.000'),
('DEMO-SUB-0005','DEMO-TASK-0003',NULL,'WORKFLOW','列出影响反应速率的因素','{"step":"list_factors"}','[]','SUCCEEDED',1,0,NULL,NULL,1,'2026-09-02 14:00:07.000','2026-09-02 14:00:08.000','2026-09-02 14:01:20.000','2026-09-02 14:01:20.000'),
('DEMO-SUB-0006','DEMO-TASK-0003','DEMO-SUB-0005','WORKFLOW','结合实验原理解释','{"step":"explain"}','["DEMO-SUB-0005"]','SUCCEEDED',2,0,NULL,NULL,1,'2026-09-02 14:01:21.000','2026-09-02 14:01:22.000','2026-09-02 14:02:00.000','2026-09-02 14:02:00.000');

-- ---------- 5. AI 消息（任务 1/2/3 各 2 条 user+assistant，共 6 条） ----------
INSERT INTO ai_message
(`id`, `conversation_id`, `task_id`, `role`, `content`, `status`, `dify_message_id`, `created_at`) VALUES
('DEMO-MSG-0001','DEMO-CONV-0001','DEMO-TASK-0001','user','帮我求解二次函数 f(x)=x^2-4x+3 的最值并画草图','SUCCEEDED','dfy-msg-0001','2026-09-01 08:00:05.000'),
('DEMO-MSG-0002','DEMO-CONV-0001','DEMO-TASK-0001','assistant','顶点式=(x-2)^2-1，最小值-1，在 x=2 处取到。','SUCCEEDED','dfy-msg-0002','2026-09-01 08:01:15.000'),
('DEMO-MSG-0003','DEMO-CONV-0002','DEMO-TASK-0002','user','对斜面上的物块做牛顿第二定律受力分析','SUCCEEDED','dfy-msg-0003','2026-09-01 10:00:05.000'),
('DEMO-MSG-0004','DEMO-CONV-0002','DEMO-TASK-0002','assistant','下滑分力 mg·sinθ，支持力 mg·cosθ，摩擦力 μ·mg·cosθ。','SUCCEEDED','dfy-msg-0004','2026-09-01 10:01:05.000'),
('DEMO-MSG-0005','DEMO-CONV-0003','DEMO-TASK-0003','user','分析锌与稀硫酸反应速率的决定因素','SUCCEEDED','dfy-msg-0005','2026-09-02 14:00:05.000'),
('DEMO-MSG-0006','DEMO-CONV-0003','DEMO-TASK-0003','assistant','浓度、温度、接触面积共同决定反应速率。','SUCCEEDED','dfy-msg-0006','2026-09-02 14:02:00.000');

-- ---------- 6. AI 证据（12 条，绑定消息） ----------
INSERT INTO ai_evidence
(`id`, `message_id`, `source_type`, `title`, `url`, `content_snippet`, `score`, `created_at`) VALUES
('DEMO-EV-0001','DEMO-MSG-0002','web','二次函数顶点式','https://example.com/quadratic','配方法化简步骤摘要',0.960000,'2026-09-01 08:01:10.000'),
('DEMO-EV-0002','DEMO-MSG-0002','web','二次函数最值公式','https://example.com/extremum','当 a>0 时开口向上有最小值',0.940000,'2026-09-01 08:01:11.000'),
('DEMO-EV-0003','DEMO-MSG-0004','web','牛顿第二定律','https://example.com/newton2','F=ma 在斜面坐标系下的分量',0.970000,'2026-09-01 10:01:00.000'),
('DEMO-EV-0004','DEMO-MSG-0004','web','斜面摩擦力','https://example.com/friction','f=μN，N=mg·cosθ',0.930000,'2026-09-01 10:01:01.000'),
('DEMO-EV-0005','DEMO-MSG-0004','web','受力分析图','https://example.com/fbd','斜面上物块的受力分解示意图',0.890000,'2026-09-01 10:01:02.000'),
('DEMO-EV-0006','DEMO-MSG-0006','web','化学反应速率','https://example.com/rate','浓度升高反应速率加快',0.950000,'2026-09-02 14:01:55.000'),
('DEMO-EV-0007','DEMO-MSG-0006','web','温度影响','https://example.com/temp','温度每升高10℃速率约翻倍',0.910000,'2026-09-02 14:01:56.000'),
('DEMO-EV-0008','DEMO-MSG-0006','web','接触面积','https://example.com/surface','粉末状固体比块状反应更快',0.880000,'2026-09-02 14:01:57.000'),
('DEMO-EV-0009','DEMO-MSG-0002','file','配方法推导笔记','https://example.com/note1.pdf','教师讲义第3章配方法',0.850000,'2026-09-01 08:01:12.000'),
('DEMO-EV-0010','DEMO-MSG-0004','file','斜面模型图','https://example.com/incline.pdf','斜面物块模型图解',0.820000,'2026-09-01 10:01:03.000'),
('DEMO-EV-0011','DEMO-MSG-0006','web','实验数据表','https://example.com/exp-data','锌粒与稀硫酸实验数据',0.900000,'2026-09-02 14:01:58.000'),
('DEMO-EV-0012','DEMO-MSG-0002','web','练习巩固题','https://example.com/practice','最值相关配套练习题',0.790000,'2026-09-01 08:01:13.000');

-- ---------- 7. AI 模型调用日志（18 行，跨 6 个任务，token/cost/latency 有值） ----------
INSERT INTO ai_model_call_log
(`id`, `task_id`, `node_name`, `model`, `total_tokens`, `latency_ms`, `cost`, `error_code`, `created_at`) VALUES
('DEMO-MCL-0001','DEMO-TASK-0001','math-llm','deepseek-chat',1200,850,0.012000,NULL,'2026-09-01 08:00:20.000'),
('DEMO-MCL-0002','DEMO-TASK-0001','summarizer','deepseek-chat',340,220,0.003400,NULL,'2026-09-01 08:01:05.000'),
('DEMO-MCL-0003','DEMO-TASK-0001','math-llm-step2','deepseek-chat',980,760,0.009800,NULL,'2026-09-01 08:01:00.000'),
('DEMO-MCL-0004','DEMO-TASK-0002','physics-llm','deepseek-chat',1500,1100,0.015000,NULL,'2026-09-01 10:00:30.000'),
('DEMO-MCL-0005','DEMO-TASK-0002','physics-llm-step2','deepseek-chat',620,430,0.006200,NULL,'2026-09-01 10:00:55.000'),
('DEMO-MCL-0006','DEMO-TASK-0003','chemistry-llm','deepseek-chat',2100,1600,0.021000,NULL,'2026-09-02 14:00:40.000'),
('DEMO-MCL-0007','DEMO-TASK-0003','chemistry-llm-step2','deepseek-chat',780,590,0.007800,NULL,'2026-09-02 14:01:40.000'),
('DEMO-MCL-0008','DEMO-TASK-0003','summarizer','deepseek-chat',410,300,0.004100,NULL,'2026-09-02 14:01:50.000'),
('DEMO-MCL-0009','DEMO-TASK-0004','math-llm','deepseek-reasoner',2600,30000,0.130000,'DIFY_TIMEOUT','2026-09-02 09:00:20.000'),
('DEMO-MCL-0010','DEMO-TASK-0005','chat-llm','deepseek-chat',1800,900,0.018000,'PARSE_ERROR','2026-09-03 16:00:12.000'),
('DEMO-MCL-0011','DEMO-TASK-0006','workflow-router','deepseek-chat',150,120,0.001500,NULL,'2026-09-03 20:00:10.000'),
('DEMO-MCL-0012','DEMO-TASK-0006','chemistry-llm','deepseek-chat',980,760,0.009800,NULL,'2026-09-03 20:00:30.000'),
('DEMO-MCL-0013','DEMO-TASK-0001','workflow-router','deepseek-chat',120,90,0.001200,NULL,'2026-09-01 08:00:10.000'),
('DEMO-MCL-0014','DEMO-TASK-0002','workflow-router','deepseek-chat',130,95,0.001300,NULL,'2026-09-01 10:00:10.000'),
('DEMO-MCL-0015','DEMO-TASK-0004','workflow-router','deepseek-chat',140,100,0.001400,NULL,'2026-09-02 09:00:10.000'),
('DEMO-MCL-0016','DEMO-TASK-0005','workflow-router','deepseek-chat',110,80,0.001100,NULL,'2026-09-03 16:00:08.000'),
('DEMO-MCL-0017','DEMO-TASK-0006','summarizer','deepseek-chat',320,240,0.003200,NULL,'2026-09-03 20:00:38.000'),
('DEMO-MCL-0018','DEMO-TASK-0001','chat-llm','deepseek-chat',520,380,0.005200,NULL,'2026-09-01 08:00:45.000');

-- ---------- 8. AI 事件（54 条：展示每任务真实时间线） ----------
-- 任务 1（18 条）、任务 2（15 条）、任务 3（20 条）已由 event_sequence 对齐；
-- 为满足 >=50 要求，另行补充跨任务事件，事件数合计 54。

INSERT INTO ai_event
(`id`, `task_id`, `subtask_id`, `sequence`, `event_type`, `status`, `payload_version`, `payload_json`, `source_event_id`, `occurred_at`) VALUES
('DEMO-EVT-0001','DEMO-TASK-0001','DEMO-SUB-0001',1,'TASK_STARTED','RUNNING',1,'{"progress":0}','e-0001','2026-09-01 08:00:06.000'),
('DEMO-EVT-0002','DEMO-TASK-0001','DEMO-SUB-0001',2,'TASK_DECOMPOSED','SUCCEEDED',1,'{"subtasks":2}','e-0002','2026-09-01 08:00:07.000'),
('DEMO-EVT-0003','DEMO-TASK-0001','DEMO-SUB-0001',3,'AGENT_ASSIGNED','SUCCEEDED',1,'{"agent":"math"}','e-0003','2026-09-01 08:00:08.000'),
('DEMO-EVT-0004','DEMO-TASK-0001','DEMO-SUB-0001',4,'SUBTASK_STARTED','RUNNING',1,'{"subtask":"DEMO-SUB-0001"}','e-0004','2026-09-01 08:00:09.000'),
('DEMO-EVT-0005','DEMO-TASK-0001','DEMO-SUB-0001',5,'MODEL_CALL','SUCCEEDED',1,'{"model":"deepseek-chat","tokens":1200}','e-0005','2026-09-01 08:00:20.000'),
('DEMO-EVT-0006','DEMO-TASK-0001','DEMO-SUB-0001',6,'SUBTASK_FINISHED','SUCCEEDED',1,'{}','e-0006','2026-09-01 08:00:40.000'),
('DEMO-EVT-0007','DEMO-TASK-0001','DEMO-SUB-0002',7,'SUBTASK_STARTED','RUNNING',1,'{"subtask":"DEMO-SUB-0002"}','e-0007','2026-09-01 08:00:42.000'),
('DEMO-EVT-0008','DEMO-TASK-0001','DEMO-SUB-0002',8,'MODEL_CALL','SUCCEEDED',1,'{"model":"deepseek-chat","tokens":980}','e-0008','2026-09-01 08:01:00.000'),
('DEMO-EVT-0009','DEMO-TASK-0001','DEMO-SUB-0002',9,'SUBTASK_FINISHED','SUCCEEDED',1,'{}','e-0009','2026-09-01 08:01:15.000'),
('DEMO-EVT-0010','DEMO-TASK-0001','DEMO-SUB-0002',10,'EVIDENCE_ADDED','SUCCEEDED',1,'{"evidence":"DEMO-EV-0001"}','e-0010','2026-09-01 08:01:16.000'),
('DEMO-EVT-0011','DEMO-TASK-0001','NULL',11,'MESSAGE_PART','SUCCEEDED',1,'{"part":"assistant","token":"顶点式"}','e-0011','2026-09-01 08:01:17.000'),
('DEMO-EVT-0012','DEMO-TASK-0001','NULL',12,'MESSAGE_FINISHED','SUCCEEDED',1,'{}','e-0012','2026-09-01 08:01:18.000'),
('DEMO-EVT-0013','DEMO-TASK-0001','NULL',13,'TASK_PROGRESS','RUNNING',1,'{"progress":80}','e-0013','2026-09-01 08:01:19.000'),
('DEMO-EVT-0014','DEMO-TASK-0001','NULL',14,'TASK_PROGRESS','RUNNING',1,'{"progress":95}','e-0014','2026-09-01 08:01:19.500'),
('DEMO-EVT-0015','DEMO-TASK-0001','NULL',15,'TASK_FINISHED','SUCCEEDED',1,'{"progress":100}','e-0015','2026-09-01 08:01:20.000'),
('DEMO-EVT-0016','DEMO-TASK-0001','DEMO-SUB-0001',16,'EVIDENCE_ADDED','SUCCEEDED',1,'{"evidence":"DEMO-EV-0009"}','e-0016','2026-09-01 08:01:21.000'),
('DEMO-EVT-0017','DEMO-TASK-0001','DEMO-SUB-0002',17,'EVIDENCE_ADDED','SUCCEEDED',1,'{"evidence":"DEMO-EV-0002"}','e-0017','2026-09-01 08:01:22.000'),
('DEMO-EVT-0018','DEMO-TASK-0001','DEMO-SUB-0002',18,'EVIDENCE_ADDED','SUCCEEDED',1,'{"evidence":"DEMO-EV-0012"}','e-0018','2026-09-01 08:01:23.000'),
-- 任务 2（15 条）
('DEMO-EVT-1001','DEMO-TASK-0002','DEMO-SUB-0003',1,'TASK_STARTED','RUNNING',1,'{"progress":0}','e-1001','2026-09-01 10:00:06.000'),
('DEMO-EVT-1002','DEMO-TASK-0002','DEMO-SUB-0003',2,'TASK_DECOMPOSED','SUCCEEDED',1,'{"subtasks":2}','e-1002','2026-09-01 10:00:07.000'),
('DEMO-EVT-1003','DEMO-TASK-0002','DEMO-SUB-0003',3,'AGENT_ASSIGNED','SUCCEEDED',1,'{"agent":"physics"}','e-1003','2026-09-01 10:00:08.000'),
('DEMO-EVT-1004','DEMO-TASK-0002','DEMO-SUB-0003',4,'SUBTASK_STARTED','RUNNING',1,'{}','e-1004','2026-09-01 10:00:09.000'),
('DEMO-EVT-1005','DEMO-TASK-0002','DEMO-SUB-0003',5,'MODEL_CALL','SUCCEEDED',1,'{"tokens":1500}','e-1005','2026-09-01 10:00:30.000'),
('DEMO-EVT-1006','DEMO-TASK-0002','DEMO-SUB-0003',6,'SUBTASK_FINISHED','SUCCEEDED',1,'{}','e-1006','2026-09-01 10:00:55.000'),
('DEMO-EVT-1007','DEMO-TASK-0002','DEMO-SUB-0004',7,'SUBTASK_STARTED','RUNNING',1,'{}','e-1007','2026-09-01 10:00:57.000'),
('DEMO-EVT-1008','DEMO-TASK-0002','DEMO-SUB-0004',8,'MODEL_CALL','SUCCEEDED',1,'{"tokens":620}','e-1008','2026-09-01 10:01:00.000'),
('DEMO-EVT-1009','DEMO-TASK-0002','DEMO-SUB-0004',9,'SUBTASK_FINISHED','SUCCEEDED',1,'{}','e-1009','2026-09-01 10:01:05.000'),
('DEMO-EVT-1010','DEMO-TASK-0002','NULL',10,'EVIDENCE_ADDED','SUCCEEDED',1,'{"evidence":"DEMO-EV-0003"}','e-1010','2026-09-01 10:01:06.000'),
('DEMO-EVT-1011','DEMO-TASK-0002','NULL',11,'EVIDENCE_ADDED','SUCCEEDED',1,'{"evidence":"DEMO-EV-0004"}','e-1011','2026-09-01 10:01:07.000'),
('DEMO-EVT-1012','DEMO-TASK-0002','NULL',12,'MESSAGE_PART','SUCCEEDED',1,'{}','e-1012','2026-09-01 10:01:08.000'),
('DEMO-EVT-1013','DEMO-TASK-0002','NULL',13,'EVIDENCE_ADDED','SUCCEEDED',1,'{"evidence":"DEMO-EV-0005"}','e-1013','2026-09-01 10:01:09.000'),
('DEMO-EVT-1014','DEMO-TASK-0002','NULL',14,'EVIDENCE_ADDED','SUCCEEDED',1,'{"evidence":"DEMO-EV-0010"}','e-1014','2026-09-01 10:01:10.000'),
('DEMO-EVT-1015','DEMO-TASK-0002','NULL',15,'TASK_FINISHED','SUCCEEDED',1,'{"progress":100}','e-1015','2026-09-01 10:01:10.000'),
-- 任务 3（20 条）
('DEMO-EVT-2001','DEMO-TASK-0003','DEMO-SUB-0005',1,'TASK_STARTED','RUNNING',1,'{"progress":0}','e-2001','2026-09-02 14:00:06.000'),
('DEMO-EVT-2002','DEMO-TASK-0003','DEMO-SUB-0005',2,'TASK_DECOMPOSED','SUCCEEDED',1,'{"subtasks":2}','e-2002','2026-09-02 14:00:07.000'),
('DEMO-EVT-2003','DEMO-TASK-0003','DEMO-SUB-0005',3,'AGENT_ASSIGNED','SUCCEEDED',1,'{"agent":"chemistry"}','e-2003','2026-09-02 14:00:08.000'),
('DEMO-EVT-2004','DEMO-TASK-0003','DEMO-SUB-0005',4,'SUBTASK_STARTED','RUNNING',1,'{}','e-2004','2026-09-02 14:00:09.000'),
('DEMO-EVT-2005','DEMO-TASK-0003','DEMO-SUB-0005',5,'MODEL_CALL','SUCCEEDED',1,'{"tokens":2100}','e-2005','2026-09-02 14:00:40.000'),
('DEMO-EVT-2006','DEMO-TASK-0003','DEMO-SUB-0005',6,'SUBTASK_FINISHED','SUCCEEDED',1,'{}','e-2006','2026-09-02 14:01:20.000'),
('DEMO-EVT-2007','DEMO-TASK-0003','DEMO-SUB-0006',7,'SUBTASK_STARTED','RUNNING',1,'{}','e-2007','2026-09-02 14:01:22.000'),
('DEMO-EVT-2008','DEMO-TASK-0003','DEMO-SUB-0006',8,'MODEL_CALL','SUCCEEDED',1,'{"tokens":780}','e-2008','2026-09-02 14:01:40.000'),
('DEMO-EVT-2009','DEMO-TASK-0003','DEMO-SUB-0006',9,'SUBTASK_FINISHED','SUCCEEDED',1,'{}','e-2009','2026-09-02 14:01:50.000'),
('DEMO-EVT-2010','DEMO-TASK-0003','NULL',10,'EVIDENCE_ADDED','SUCCEEDED',1,'{"evidence":"DEMO-EV-0006"}','e-2010','2026-09-02 14:01:55.000'),
('DEMO-EVT-2011','DEMO-TASK-0003','NULL',11,'EVIDENCE_ADDED','SUCCEEDED',1,'{"evidence":"DEMO-EV-0007"}','e-2011','2026-09-02 14:01:56.000'),
('DEMO-EVT-2012','DEMO-TASK-0003','NULL',12,'EVIDENCE_ADDED','SUCCEEDED',1,'{"evidence":"DEMO-EV-0008"}','e-2012','2026-09-02 14:01:57.000'),
('DEMO-EVT-2013','DEMO-TASK-0003','NULL',13,'EVIDENCE_ADDED','SUCCEEDED',1,'{"evidence":"DEMO-EV-0011"}','e-2013','2026-09-02 14:01:58.000'),
('DEMO-EVT-2014','DEMO-TASK-0003','NULL',14,'MESSAGE_PART','SUCCEEDED',1,'{}','e-2014','2026-09-02 14:01:59.000'),
('DEMO-EVT-2015','DEMO-TASK-0003','NULL',15,'MESSAGE_FINISHED','SUCCEEDED',1,'{}','e-2015','2026-09-02 14:02:00.000'),
('DEMO-EVT-2016','DEMO-TASK-0003','NULL',16,'TASK_PROGRESS','RUNNING',1,'{"progress":85}','e-2016','2026-09-02 14:02:01.000'),
('DEMO-EVT-2017','DEMO-TASK-0003','NULL',17,'TASK_PROGRESS','RUNNING',1,'{"progress":98}','e-2017','2026-09-02 14:02:02.000'),
('DEMO-EVT-2018','DEMO-TASK-0003','NULL',18,'TASK_PROGRESS','RUNNING',1,'{"progress":100}','e-2018','2026-09-02 14:02:03.000'),
('DEMO-EVT-2019','DEMO-TASK-0003','NULL',19,'TASK_FINISHED','SUCCEEDED',1,'{}','e-2019','2026-09-02 14:02:04.000'),
('DEMO-EVT-2020','DEMO-TASK-0003','DEMO-SUB-0005',20,'MODEL_CALL','SUCCEEDED',1,'{"tokens":410}','e-2020','2026-09-02 14:02:05.000'),
-- 任务 4（失败，4 条）
('DEMO-EVT-3001','DEMO-TASK-0004','NULL',1,'TASK_STARTED','RUNNING',1,'{"progress":0}','e-3001','2026-09-02 09:00:06.000'),
('DEMO-EVT-3002','DEMO-TASK-0004','NULL',2,'MODEL_CALL','RUNNING',1,'{"tokens":2600}','e-3002','2026-09-02 09:00:20.000'),
('DEMO-EVT-3003','DEMO-TASK-0004','NULL',3,'EXECUTION_INTERRUPTED','FAILED',1,'{"code":"DIFY_TIMEOUT"}','e-3003','2026-09-02 09:00:30.000'),
('DEMO-EVT-3004','DEMO-TASK-0004','NULL',4,'TASK_FAILED','FAILED',1,'{"code":"DIFY_TIMEOUT","retryable":true}','e-3004','2026-09-02 09:00:30.500'),
-- 任务 5（失败，3 条）
('DEMO-EVT-4001','DEMO-TASK-0005','NULL',1,'TASK_STARTED','RUNNING',1,'{"progress":0}','e-4001','2026-09-03 16:00:06.000'),
('DEMO-EVT-4002','DEMO-TASK-0005','NULL',2,'MODEL_CALL','RUNNING',1,'{"tokens":1800}','e-4002','2026-09-03 16:00:12.000'),
('DEMO-EVT-4003','DEMO-TASK-0005','NULL',3,'PARSE_ERROR','FAILED',1,'{}','e-4003','2026-09-03 16:00:20.000'),
-- 任务 6（运行中，3 条）
('DEMO-EVT-5001','DEMO-TASK-0006','NULL',1,'TASK_STARTED','RUNNING',1,'{"progress":0}','e-5001','2026-09-03 20:00:06.000'),
('DEMO-EVT-5002','DEMO-TASK-0006','NULL',2,'TASK_DECOMPOSED','SUCCEEDED',1,'{}','e-5002','2026-09-03 20:00:08.000'),
('DEMO-EVT-5003','DEMO-TASK-0006','NULL',3,'MODEL_CALL','RUNNING',1,'{"tokens":980}','e-5003','2026-09-03 20:00:30.000');

-- =============================================================================
-- 汇总（便于人工核对）：
--   ai_task            6 条（3 SUCCEEDED + 2 FAILED + 1 RUNNING）      >= 5   ✓
--   ai_subtask         6 条                                              -     ✓
--   ai_message         6 条（3 user + 3 assistant）                      -     ✓
--   ai_evidence       12 条                                            >= 10  ✓
--   ai_model_call_log 18 条                                            >= 12  ✓
--   ai_event          54 条（18+15+20+4+3+3）                          >= 50  ✓
--   模型日志页 12+ 行真实数据；学情页有分布/趋势；回放有真实时间线。
-- =============================================================================
