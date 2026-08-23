# 灵犀教育 — 多智能体协同学习系统改造实施计划

> 以 `LingXi_Dify_前后端开发_Task_List.xlsx` 原始编号为准，
> 参考 `LingXi_Dify_后端详细实施方案.md` 和 `LingXi_Dify_前端详细实施方案.md`。
> 允许将原始任务拆分为子任务，子任务编号格式：`{原始编号}-{序号}`。

---

## P0 — 基础安全（8 项）

### SEC-01：轮换所有已暴露的密钥和凭据

- [x] SEC-01-1：检查前端 .env、config/api.js 中是否还有 Dify Key
  - 结果：前端 `.env` 已 gitignore，`config/api.js` 使用 `process.env.VUE_APP_*`，无硬编码
- [x] SEC-01-2：检查后端 application.properties 中是否还有硬编码凭据
  - 结果：所有凭据使用 `${VAR}` 占位符，通过系统环境变量注入
- [x] SEC-01-3：检查 git 历史中是否已提交过密钥，清理或轮换
  - 结果：后端 `7b1db74` + `29644de`，前端 `f7e69fa`，已轮换并推送
- [x] SEC-01-4：确认 .env 配置正确的 Dify/数据库/云服务凭据
  - 结果：前端 `.env` 本地配置正确，后端通过系统环境变量注入

### BE-01：实现统一 Dify 网关

> 已在初始代码中完成（DifyGateway + DifyGatewayImpl + DifyChatApplication 枚举），不需要再做。

### BE-02：定义统一任务接口

- [x] **BE-02-1：创建 AiTaskController**（POST /api/ai/task）
  - `controller/AiTaskController.java` - 构造函数注入
  - `@Login` 鉴权 + 参数校验（query 非空、TokenEntity 非空）
  - 异常处理：`DifyGatewayException` 优先捕获，兜底 500
  - 响应：`ResultConstant.success(AiTaskResponse)`
- [x] **BE-02-2：创建 AiTaskService**（调度层）
  - `service/AiTaskService.java` + `service/impl/AiTaskServiceImpl.java`
  - 注入 `DifyGateway` + `AiAgentRouter`
  - 将 `AiTaskRequest` 转为 `DifyChatflowRequest`，委托 `DifyGateway.sendChatMessage`
  - 返回 `AiTaskResponse`（从 Dify JsonNode 提取 answer/conversationId/messageId/elapsedMs）
  - 统计 API 调用耗时
- [x] **BE-02-3：Agent 路由骨架**
  - `gateway/AiAgentRouter.java`（`@Component`）
  - `route(query)` 方法，默认返回 `DifyChatApplication.CHATFLOW`
  - TODO：Dify 平台配好学科 Agent 后，根据 query 关键词路由到不同应用
- [x] **BE-02-4：统一响应结构**
  - `domain/dto/AiTaskResponse.java` — 封装 `answer` / `conversationId` / `messageId` / `elapsedMs`
  - 前端无需解析 Dify 原始 JsonNode，直接消费结构化 VO

### BE-03：建立登录用户与 Dify user 映射

- [ ] BE-03-1：确认 TokenEntity.getId() 作为 Dify userId 的稳定性
- [ ] BE-03-2：检查 DifyGatewayImpl 中 userId 传递链路是否完整

### BE-04：修改会话资源所有权校验

- [ ] BE-04-1：检查 ConversationController 中 list/detail/edit/delete 接口是否有越权查询
- [ ] BE-04-2：所有新接口已通过 @Login 鉴权（已满足）

### BE-05：定义前端格式化事件协议

- [x] **BE-05-1：创建 AiEvent 实体 + 数据库表**
  - 新建 pojo/AiEvent.java
  - 新建 mapper/AiEventMapper.java + AiEventMapper.xml
  - 新建 service/AiEventService.java + impl/AiEventServiceImpl.java
  - 新建 controller/AiEventController.java（GET /api/ai/events）
  - DDL：lx_ai_event 表
- [ ] **BE-05-2：扩展事件类型**
  - 当前支持：task_start / task_complete / task_error
  - 后续需要覆盖：task_decomposed / agent_assigned / node_progress / answer_delta 等

### FE-01：移除前端 Dify Key 和直连调用

- [x] **FE-01-1：重写 TeachingAssistant.vue**
  - 去掉 VUE_APP_AI_KEY 引用
  - 去掉 fetch(AI_API/v1/chat-messages) 直连
  - 改用 baseRequest.post(/api/ai/task) 调后端
  - 保留原有 UI 结构
- [ ] **FE-01-2：清理前端 Dify 配置残留**
  - config/api.js 中的 AI_API 是否还需保留
  - utils/api.js 中的 aiRequest 是否删除
  - .env 中 VUE_APP_AI_KEY / VUE_APP_AI_API 是否清理

### FE-02：移除教学方案中的固定 Dify iframe

- [x] **FE-02-1：创建 DifyProxyController**（GET /api/proxy/dify/**）
  - 302 重定向到 Dify 服务器
  - Dify URL 从后端配置读取
- [x] **FE-02-2：修改 5 处 iframe src**
  - TeachingPlan.vue / TeachingPlanPro.vue / AIQuiz.vue / SelfTestAssistant.vue / GradeHomework.vue
  - 全部改为 /api/proxy/dify/... 路径

---

## P1 — MVP 主链路（15 项）

### BE-06：实现 SSE 流式代理

- [ ] BE-06-1：DifyGatewayImpl 新增 streaming 模式（response_mode: streaming）
- [ ] BE-06-2：新增 GET /api/ai/tasks/{taskId}/events（SseEmitter）
- [ ] BE-06-3：心跳 + 超时断开 + 客户端断开清理

### BE-07：支持任务停止和断线恢复

- [ ] BE-07-1：POST /api/ai/tasks/{taskId}/stop 接口
- [ ] BE-07-2：Last-Event-ID 续传逻辑
- [ ] BE-07-3：失败子任务重试

### BE-08：新建 AI 6 张数据表

- [x] BE-08-1：lx_ai_event 表（已完成）
- [ ] BE-08-2：lx_ai_task 表
- [ ] BE-08-3：lx_ai_subtask 表
- [ ] BE-08-4：lx_ai_message 表
- [ ] BE-08-5：lx_ai_evidence 表
- [ ] BE-08-6：lx_ai_model_call_log 表

### BE-09：持久化任务拆解和执行轨迹

- [ ] BE-09-1：AiTask / AiSubtask 实体 + Mapper + Service
- [ ] BE-09-2：任务拆解结果持久化
- [ ] BE-09-3：Agent 分配记录持久化
- [ ] BE-09-4：节点状态和异常持久化

### BE-10：持久化消息和引用证据

- [ ] BE-10-1：AiMessage 实体 + Mapper + Service
- [ ] BE-10-2：AiEvidence 实体 + Mapper + Service
- [ ] BE-10-3：按会话分页查询

### BE-11：实现统一会话管理接口

- [ ] BE-11-1：确认现有 Conversation CRUD 是否满足需求
- [ ] BE-11-2：补充分页/重命名/软删除接口

### BE-12：建立 Dify 多应用版本管理

- [ ] BE-12-1：DifyChatApplication 枚举扩展
- [ ] BE-12-2：支持工作流版本回退

### FE-03：建立前端 AI 会话状态管理

- [ ] FE-03-1：Vuex ai 模块
- [ ] FE-03-2：归一化状态 + 路由参数恢复

### FE-04：重写前端 SSE 解析器

- [ ] FE-04-1：新建 services/SseClient.js
- [ ] FE-04-2：分片/粘包/CRLF/重连/游标/去重

### FE-05：完成任务工作台整体框架

- [ ] FE-05-1：新建 views/student/AiAssistantPage.vue
- [ ] FE-05-2：总体状态/停止/重试操作

### FE-06：完成认知轨迹时间线

- [ ] FE-06-1：认知轨迹时间线组件
- [ ] FE-06-2：节点详情展开

### FE-07：完成子任务与 Agent 面板

- [ ] FE-07-1：子任务列表/状态/Agent 标签
- [ ] FE-07-2：失败子任务重试入口

### FE-08：完成证据面板和引用标记

- [ ] FE-08-1：证据抽屉组件
- [ ] FE-08-2：来源标记/置信度/局限性展示

### FE-09：完善对话框输入区域

- [ ] FE-09-1：安全 Markdown 渲染
- [ ] FE-09-2：文件上传 / 停止生成 / 重新生成 / 错误提示

### FE-10：接入真实会话历史

- [ ] FE-10-1：会话分页列表
- [ ] FE-10-2：历史消息加载 / 重命名 / 删除
- [ ] FE-10-3：刷新恢复

---

## P2 — 增强与验收（6 项）

### BE-13：建立模型调用日志和指标

- [ ] BE-13-1：DifyGatewayImpl 添加每次调用的 Token/耗时/模型名记录
- [ ] BE-13-2：按用户/时间/Agent 维度查询
- [ ] BE-13-3：比赛演示数据导出

### BE-14：完成隐私脱敏和数据治理

- [ ] BE-14-1：日志脱敏（不记录 Dify Key、明文密码等）
- [ ] BE-14-2：审计/限流/数据保留与删除

### FE-11：完成教师分析页和任务回放

- [ ] FE-11-1：任务回放视图（认知轨迹 + Token + 耗时）
- [ ] FE-11-2：多智能体调用统计图表

### QA 测试

- [ ] QA-01：后端集成测试（SSE / 权限 / 恢复 / 失败 / 超时）
- [ ] QA-02：前端测试（SSE / Store / 组件 / E2E）
- [ ] QA-03：压力测试（多用户并发 / 长连接 / 大文件）

---

## 设计原则

1. **不改原有业务代码** — 新增文件实现新功能
2. **原有 iframe 保持可用** — 通过代理透传
3. **现有 DifyGateway 不动** — 在其上加调度层和日志埋点
4. **Java 8 / Vue 3 JS / 无 Spring Security** — 不升级
5. **前后端分开提交** — 各自 repo，独立推送
