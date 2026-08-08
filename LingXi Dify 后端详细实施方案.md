# LingXi Dify 后端详细实施方案

本文面向 LingXi 后端开发、测试和运维人员，目标是在不改变现有教学业务主体结构的前提下，将当前 Dify 简单聊天代理升级为可管理、可追踪、可恢复、可审计的多智能体任务服务。实施范围对应多维表格中的 SEC\-01、BE\-01 至 BE\-14、QA\-01 和 QA\-03。

[https://vcnx845om8z0.feishu.cn/base/PjyZby6DJaxxgJsaAl8cSHUznVg]()

# 1\. 现状与改造目标

当前后端通过 Spring Boot Controller 和 RestTemplate 直接调用 Dify，聊天请求使用 blocking 模式，接口以原始字符串或 JSONObject 形式透传结果。Dify 地址和密钥散落在 ApiServiceImpl 与 application\.properties 中；Conversation 数据结构只保存会话标识和少量审计字段，尚未覆盖任务、子任务、事件、证据、模型调用和恢复信息。部分会话接口直接使用调用方传入的查询条件或记录 ID，缺少基于当前登录用户的强制所有权过滤。

本次改造完成后，LingXi 后端应成为唯一 Dify 访问入口。前端只识别 LingXi 的任务和事件协议，不感知 Dify API Key，不依赖 Dify 原始事件结构。系统能够创建多智能体任务、持续推送认知轨迹、持久化执行过程、停止或恢复任务，并提供面向教师分析和问题排查的模型调用日志。

|现状位置|主要问题|目标状态|
|---|---|---|
|ApiServiceImpl\.java|地址、密钥写死；blocking 请求；返回原始字符串|配置外置、流式调用、类型化 DTO 与统一错误模型|
|ApiController\.java|Controller 混合业务编排、远程调用和 JSON 解析|Controller 只负责认证、参数校验和响应适配|
|Conversation\.java / Mapper|仅保存基础会话元数据|任务、子任务、消息、事件、证据和模型日志独立建模|
|application\.properties|存在敏感凭据和环境耦合|使用环境变量或密钥服务，并完成旧凭据轮换|

# 2\. 目标后端架构

后端采用“任务 API 层—任务编排层—Dify 适配层—事件与持久化层”的分层方式。Controller 不直接拼装 Dify 请求；Dify 事件先由适配器解析，再转换为 LingXiEvent 后写入事件存储并推送给前端。数据库保存业务事实，Redis 只用于短期连接状态、事件游标、幂等键和限流数据。

|模块|职责|建议实现|
|---|---|---|
|AiTaskController|任务创建、详情、停止、重试和事件订阅|统一挂载在 /api/ai 下，所有接口从登录上下文读取用户|
|AiTaskService|任务状态机、幂等控制和业务编排|状态只允许按定义路径迁移，数据库更新使用乐观锁|
|DifyGateway|封装聊天、工作流、文件、会话和停止接口|集中处理鉴权、超时、重试、错误码和 Dify user 映射|
|DifyEventAdapter|解析 Dify SSE 并映射 LingXi 事件|未知事件可记录但不能导致流中断|
|AiEventService|事件排序、持久化、广播和断线续传|每个任务维护单调递增 sequence|
|AiAuditService|模型调用、Token、耗时、费用和异常日志|敏感字段脱敏，支持教师和管理员按权限查询|

# 3\. 接口契约

接口响应统一使用业务状态码、可读消息、requestId 和 data。异步任务创建成功后立即返回 LingXi taskId；Dify task\_id、conversation\_id 和 message\_id 作为外部标识存入数据库，不直接作为业务主键暴露。

|路径|方法|用途|关键返回|
|---|---|---|---|
|/api/ai/tasks|POST|创建聊天或工作流任务|taskId、conversationId、status、eventUrl|
|/api/ai/tasks/\{taskId\}|GET|查询任务快照|状态、进度、子任务、最终结果|
|/api/ai/tasks/\{taskId\}/events|GET|订阅 SSE 事件|text/event\-stream；支持 Last\-Event\-ID|
|/api/ai/tasks/\{taskId\}/stop|POST|停止正在运行的任务|停止受理状态和最终任务状态|
|/api/ai/tasks/\{taskId\}/subtasks/\{subtaskId\}/retry|POST|重试失败子任务|新的执行轮次和事件游标|
|/api/ai/conversations|GET|分页查询当前用户会话|会话摘要、最后消息、任务状态|
|/api/ai/conversations/\{id\}/messages|GET|分页读取消息与引用|消息、附件、证据、Agent 结果|
|/api/ai/conversations/\{id\}|PATCH / DELETE|重命名或删除会话|更新结果；删除采用软删除策略|

# 4\. 流式事件协议

所有事件至少包含 eventId、sequence、eventType、taskId、conversationId、occurredAt、status 和 payload。前端只根据 eventType 和 payloadVersion 分发事件。服务端必须先持久化关键状态事件，再向客户端发送，确保断线后可以按 sequence 续传。

|事件类型|触发时机|主要数据|
|---|---|---|
|task\_started|任务进入运行态|问题摘要、工作流版本、开始时间|
|task\_decomposed|任务拆解完成|子任务列表、依赖关系|
|agent\_assigned|子任务分配 Agent|subtaskId、agentType、目标|
|node\_progress|Dify 节点开始或结束|节点名称、状态、耗时、摘要|
|retrieval\_finished|知识检索完成|来源、片段、相关度、引用标识|
|validation\_finished|结果验证完成|校验项、结论、修订建议|
|answer\_delta|最终答案增量生成|文本片段和累计序号|
|task\_finished|任务终态|最终结果、Token、耗时、完成状态|
|task\_error|发生可见错误|错误分类、可重试标记和用户提示|

# 5\. 数据模型

|数据表|核心字段|约束与用途|
|---|---|---|
|ai\_task|id、user\_id、conversation\_id、type、status、progress、dify\_task\_id、version|业务任务主表；user\_id 与状态建立索引；version 用于乐观锁|
|ai\_subtask|task\_id、parent\_id、agent\_type、dependency\_json、status、retry\_count|保存任务拆解和依赖；重试产生新的 execution\_no|
|ai\_message|conversation\_id、task\_id、role、content、status、dify\_message\_id|保存用户问题和最终回答；外部消息标识唯一|
|ai\_event|task\_id、sequence、event\_type、payload\_json、occurred\_at|task\_id 与 sequence 联合唯一，用于重放和断线续传|
|ai\_evidence|message\_id、source\_type、title、url、content\_snippet、score|保存知识库和外部来源引用，正文按权限脱敏|
|ai\_model\_call\_log|task\_id、node\_name、model、tokens、latency\_ms、cost、error\_code|用于成本、性能、故障和教师分析，不保存密钥|

# 6\. 分阶段实施

## 6\.1 基础安全阶段

先关闭凭据泄露和越权入口，再建设新链路。旧接口暂时保留时必须通过兼容层调用 DifyGateway，不允许同时维护第二套 Dify 访问逻辑。

* [ ] SEC\-01：轮换 Dify、数据库和云服务凭据，清理仓库及构建产物。

* [x] BE\-01：实现 DifyGateway，并将所有 Dify API 调用迁入后端。

* [x] BE\-02：确定任务接口和统一响应结构。

* [x] BE\-03：建立登录用户与 Dify user 映射。

* [x] BE\-04：补齐会话、任务和消息的所有权校验。

* [ ] BE\-05：冻结 LingXiEvent v1 协议并提供示例事件。

## 6\.2 MVP 主链路阶段

该阶段完成“创建任务—接收 Dify 流—持久化轨迹—推送前端—恢复会话”的闭环。每完成一项都应同步提供契约测试，避免前后端在事件字段上反复联调。

* [ ] BE\-06：基于 SseEmitter 实现流式代理、心跳、超时与断开清理。

* [ ] BE\-07：实现停止、Last\-Event\-ID 续传和子任务重试。

* [ ] BE\-08：执行数据库迁移并建立索引、唯一约束和回滚脚本。

* [ ] BE\-09：持久化任务拆解、Agent 分配、节点状态和异常。

* [ ] BE\-10：持久化消息、附件和引用证据。

* [ ] BE\-11：完成统一会话管理接口。

* [ ] BE\-12：建立工作流、提示词和应用配置版本管理。

## 6\.3 增强与验收阶段

* [ ] BE\-13：建立模型调用日志、Token、成本和耗时指标。

* [ ] BE\-14：完成脱敏、审计、限流、数据保留和删除机制。

* [ ] QA\-01：完成流式、权限、恢复、失败和超时集成测试。

* [ ] QA\-03：完成多用户并发、长连接和大文件性能测试。

# 7\. 关键实现规则

**状态一致性。**任务状态建议限定为 CREATED、RUNNING、SUCCEEDED、FAILED、STOPPED 和 PARTIAL\_SUCCESS。状态更新与关键事件写入放在同一事务边界内；Dify 回调或流事件重复到达时使用外部事件标识和 sequence 保证幂等。

**连接管理。**SSE 连接只承担推送，不作为任务事实来源。客户端断开后任务是否继续运行由任务策略决定；服务端保存最近确认游标，连接恢复后先补历史事件，再切换到实时事件。

**错误分层。**区分参数错误、权限错误、Dify 限流、Dify 超时、模型失败、工作流失败和系统错误。返回给用户的消息不得包含密钥、内部地址、SQL 或堆栈，完整异常写入受控日志。

**兼容迁移。**现有 /api/chatMessage 可以保留一个版本周期，但内部必须转换为新任务调用；前端迁移完成后下线旧接口，并清理 ApiController 中的远程调用逻辑。

# 8\. 测试与验收

|测试层级|覆盖内容|通过标准|
|---|---|---|
|单元测试|事件解析、状态机、权限判断、幂等和错误映射|核心分支全部覆盖，未知 Dify 事件不会中断任务|
|契约测试|任务 API、SSE 事件字段和 payloadVersion|与前端共享 fixture，协议变更能被 CI 发现|
|集成测试|Mock Dify 流、停止、超时、断线续传和重复事件|任务终态、数据库记录和前端事件保持一致|
|安全测试|越权查询、删除、停止、日志泄露和输入注入|跨用户访问全部拒绝，响应和日志无敏感凭据|
|性能测试|并发任务、长时间 SSE、事件积压和文件上传|形成基线指标，达到团队确定的容量目标且无连接泄漏|

# 9\. 发布与回滚

发布时先部署数据库兼容迁移和后端新接口，再灰度切换前端。新旧接口并存期间记录调用量和失败率，确认所有客户端完成迁移后再移除旧路径。数据库迁移必须同时提供回滚脚本；事件协议通过 payloadVersion 演进，不直接修改已发布字段语义。Dify 工作流和提示词配置保留上一稳定版本，出现异常时可以单独回滚工作流而不回滚整个后端。

# 10\. 完成定义

后端方案完成的判定标准是：浏览器不存在 Dify 密钥或直连请求；任务能够流式执行、停止和断线恢复；多智能体轨迹、消息、证据和模型日志可查询；所有资源接口均按登录用户隔离；目标测试通过并形成性能基线。任务状态与实施进展以关联多维表格为准。

