# 灵犀教育后端（LingXi Backend）

更新日期：2026-09-12

灵犀教育后端基于 Spring Boot、MyBatis、MySQL 和 Redis，负责用户认证、班级与作业等业务、多智能体 AI 任务链路（任务/事件/SSE/会话/证据/模型日志/反馈/隐私），并作为前端访问 Dify Chatflow 和 Workflow 的统一中转层。

```text
LingXi-Frontend
  -> LingXi-Backend（登录校验、业务处理）
  -> MySQL / Redis / Dify
  -> LingXi-Backend 统一响应
  -> LingXi-Frontend
```

前端不得保存或直接使用数据库密码、Dify API Key 等后端密钥。

## 技术栈

| 组件 | 版本或说明 |
|---|---|
| Java | 目标字节码 1.8；运行/测试推荐 JDK 8/11/17（21/23 未经验证） |
| Spring Boot | 2.6.13 |
| MyBatis | 3.4.6 |
| MySQL | 8.0+ |
| Redis | 5.0+ |
| Maven | 3.6+ |
| 默认端口 | 5678 |

## 项目结构

```text
LingXi-Backend/
├── src/main/java/com/lxe/lx/
│   ├── annotation/      # @Login / @TeacherOnly / @RateLimit
│   ├── config/          # 数据源、Redis、鉴权、审计、限流、跨域
│   ├── controller/      # HTTP 接口（业务 + /api/ai/**）
│   ├── domain/          # DTO、QO、VO、LingXiEvent
│   ├── exception/       # AI 接口统一异常处理
│   ├── gateway/         # DifyGateway、DifyEventAdapter、AiAgentRouter
│   ├── mapper/          # MyBatis Mapper 接口
│   ├── pojo/            # 业务与 AI 实体
│   ├── ratelimit/       # Redis / 内存限流计数
│   ├── service/         # 业务、AI 任务、会话、反馈、隐私、Dify 中转
│   └── util/            # 工具（含 PII 脱敏）
├── src/main/resources/
│   ├── application.properties
│   ├── logback-spring.xml   # 日志 PII 脱敏
│   └── mybatis/         # MyBatis XML
├── sql/migrations/      # AI 表版本化迁移（up/down/verify）
├── sql/seed/            # 演示种子数据（DEMO 前缀，幂等）
├── lx.sql               # 11 张基础表
├── lx_add.sql           # 2 张作业表及演示数据
├── .env.example         # 环境变量模板
├── test-dify-chatflow.ps1
└── test-dify-workflow.ps1
```

## 环境配置

在后端根目录复制模板：

```bash
cp .env.example .env
```

Windows PowerShell：

```powershell
Copy-Item .env.example .env
```

编辑 `.env`，填写真实配置：

```properties
DB_URL=jdbc:mysql://<数据库地址>:3306/lx?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8&allowMultiQueries=true&autoReconnect=true&autoReconnectForPools=true
DB_USERNAME=<数据库账号>
DB_PASSWORD=<数据库密码>

REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=<Redis密码或留空>
REDIS_DATABASE=0

ALIYUN_SMS_ACCESS_KEY_ID=<短信AccessKey ID>
ALIYUN_SMS_ACCESS_KEY_SECRET=<短信AccessKey Secret>

API_BASE_URL=http://<旧版Dify地址>:<端口>/v1
API_KEY=<旧版AI接口密钥>

DIFY_CHATFLOW_BASE_URL=http://<Dify地址>:<端口>/v1
DIFY_CHATFLOW_API_KEY=<Chatflow API Key>
DIFY_WORKFLOW_BASE_URL=http://<Dify地址>:<端口>/v1
DIFY_WORKFLOW_API_KEY=<Workflow API Key>

DIFY_CONNECT_TIMEOUT_MS=5000
DIFY_READ_TIMEOUT_MS=120000
```

应用从运行目录读取 `.env`。修改 `.env` 后必须重启后端。`.env` 已被 Git 忽略，不得提交真实密码或 Key。

## 初始化数据库

创建数据库：

```sql
CREATE DATABASE IF NOT EXISTS lx
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_general_ci;
```

导入基础表：

```bash
mysql --default-character-set=utf8mb4 -h <数据库地址> -u <数据库账号> -p lx < lx.sql
```

`lx_add.sql` 包含两张作业表，同时包含演示数据：

```text
lx_homework_assignment
lx_homework_submission
```

开发演示环境可以完整导入：

```bash
mysql --default-character-set=utf8mb4 -h <数据库地址> -u <数据库账号> -p lx < lx_add.sql
```

> 导入含中文的 SQL 时必须带 `--default-character-set=utf8mb4`，否则可能报 `ERROR 1366 Incorrect string value`。

空白环境只执行 `lx_add.sql` 前面的两个 `CREATE TABLE`，不要执行后续 `INSERT INTO`。

初始化完成后应有 13 张业务表：

```text
lx_class
lx_class_grouping
lx_conversation
lx_customer
lx_document
lx_grade
lx_homework
lx_homework_assignment
lx_homework_submission
lx_study_group
lx_study_group_member
lx_study_group_message
lx_token
```

## 初始化 AI 表与演示数据

AI 表通过 `sql/migrations` 下的版本化脚本创建，按文件名时间顺序执行 `up`，再执行对应的 `verify`：

```bash
mysql --default-character-set=utf8mb4 -h <数据库地址> -u <数据库账号> -p lx < sql/migrations/20260809_be08_ai_tables_up.sql
mysql --default-character-set=utf8mb4 -h <数据库地址> -u <数据库账号> -p lx < sql/migrations/20260824_be11_ai_conversation_up.sql
mysql --default-character-set=utf8mb4 -h <数据库地址> -u <数据库账号> -p lx < sql/migrations/20260828_be10gaps_up.sql
mysql --default-character-set=utf8mb4 -h <数据库地址> -u <数据库账号> -p lx < sql/migrations/20260828_be12_ai_config_up.sql
mysql --default-character-set=utf8mb4 -h <数据库地址> -u <数据库账号> -p lx < sql/migrations/20260828_be14_privacy_audit_up.sql
mysql --default-character-set=utf8mb4 -h <数据库地址> -u <数据库账号> -p lx < sql/migrations/20260904_be01_version_type_up.sql
mysql --default-character-set=utf8mb4 -h <数据库地址> -u <数据库账号> -p lx < sql/migrations/20260905_be01_conversation_version_up.sql
mysql --default-character-set=utf8mb4 -h <数据库地址> -u <数据库账号> -p lx < sql/migrations/20260912_be15_ai_feedback_up.sql
# 每个 *_up.sql 都有对应 *_verify.sql，执行后核对表和索引
```

导入演示数据（幂等，可重复执行；只操作 `DEMO` 前缀数据，供模型日志 / 学情 / 轨迹回放使用）：

```bash
mysql --default-character-set=utf8mb4 -h <数据库地址> -u <数据库账号> -p lx < sql/seed/demo_ai_data.sql
```

AI 表共 10 张：`ai_task`、`ai_subtask`、`ai_event`、`ai_message`、`ai_evidence`、`ai_model_call_log`、`ai_conversation`、`ai_config`、`ai_audit_log`、`ai_feedback`。

## 启动 Redis

Redis 用于保存登录 Token。Redis 未运行时，受保护接口无法完成鉴权。

### Windows 原生（推荐，稳定）

```powershell
winget install --id Redis.Redis -e
Start-Process "C:\Program Files\Redis\redis-server.exe" "C:\Program Files\Redis\redis.windows.conf"
& "C:\Program Files\Redis\redis-cli.exe" ping
```

### Windows + WSL Ubuntu

```bash
sudo apt update
sudo apt install redis-server -y
sudo service redis-server start
redis-cli ping
```

> WSL2 的 `localhost` 转发在部分机器上不稳定；若后端偶发 `Redis command timed out`，改用上面的原生 Redis。

### macOS

```bash
brew install redis
brew services start redis
redis-cli ping
```

### Linux

```bash
sudo systemctl enable --now redis-server
redis-cli ping
```

返回 `PONG` 表示 Redis 正常。默认配置为 `localhost:6379`、数据库 `0`、无密码；生产环境必须设置密码并限制访问来源。

## 启动后端

### IDE 启动（推荐）

使用 IntelliJ IDEA 或 VSCode 打开 `LingXi-Backend`，运行：

```text
src/main/java/com/lxe/lx/LxApplication.java
```

IDE 的工作目录必须是 `LingXi-Backend`，否则应用无法读取根目录的 `.env`。

### Windows PowerShell

当前 `pom.xml` 将 Spring Boot Maven 插件配置为 `skip`，普通的 `mvn spring-boot:run` 会编译后退出。使用以下命令：

```powershell
mvn -DskipTests compile
mvn -q dependency:build-classpath "-Dmdep.outputFile=target/runtime-classpath.txt"
$classpath = "target\classes;" + (Get-Content target\runtime-classpath.txt -Raw).Trim()
java -cp $classpath com.lxe.lx.LxApplication
```

### macOS / Linux

```bash
mvn -DskipTests compile
mvn -q dependency:build-classpath -Dmdep.outputFile=target/runtime-classpath.txt
java -cp "target/classes:$(cat target/runtime-classpath.txt)" com.lxe.lx.LxApplication
```

启动成功日志：

```text
Tomcat started on port(s): 5678
Started LxApplication
```

检查端口：

```powershell
Test-NetConnection 127.0.0.1 -Port 5678
```

```bash
curl -i -X POST http://localhost:5678/token/login \
  -H "Content-Type: application/json" \
  -d '{}'
```

能够收到 JSON 参数提示即表示后端接口可达。

## 前后端联调

前后端在同一台电脑时，前端可以使用：

```properties
VUE_APP_BASE_API=http://localhost:5678
```

前后端在不同电脑时，`localhost` 指向前端自己的电脑，必须改成后端电脑的局域网 IP 或服务器域名：

```properties
VUE_APP_BASE_API=http://<后端电脑IP>:5678
```

同时确认防火墙或云服务器安全组允许访问 TCP 5678。修改前端环境变量后需要重启前端开发服务器。

## 响应与鉴权

普通成功响应：

```json
{
  "status": 200,
  "msg": "success",
  "data": {}
}
```

需要登录的接口通过 Header 传递 Token：

```http
token: <POST /token/login 返回的 Token>
```

Token 缺失或过期时返回 HTTP 401：

```json
{
  "status": 1000,
  "msg": "登录过期，请重新登录"
}
```

前端必须同时判断 HTTP 状态码和响应体中的业务 `status`。

## 账号接口

登录和注册同时支持以下两种请求格式，推荐新客户端使用 JSON：

```text
application/json
application/x-www-form-urlencoded
```

使用表单格式时，Body 必须是 `userId=...&password=...` 形式，不能发送 JSON 字符串。

### 注册

```http
POST /customer/add
Content-Type: application/json
```

```json
{
  "userId": "student001",
  "name": "测试学生",
  "email": "student001@example.com",
  "phoneNumber": "13900000001",
  "password": "123456",
  "state": "2"
}
```

`state=1` 表示教师，`state=2` 表示学生。

表单请求使用相同字段，例如：

```text
userId=student001&name=测试学生&email=student001%40example.com&phoneNumber=13900000001&password=123456&state=2
```

### 登录

```http
POST /token/login
Content-Type: application/json
```

```json
{
  "userId": "student001",
  "password": "123456"
}
```

成功响应的 `data` 是登录 Token。

表单请求示例：

```text
userId=student001&password=123456
```

## Chatflow 中转

### 发送消息

```http
POST /api/chatflow/messages
Content-Type: application/json
token: <登录Token>
```

```json
{
  "query": "1+1等于多少？",
  "inputs": {},
  "conversationId": "",
  "files": [],
  "autoGenerateName": true
}
```

成功响应中的：

```text
data.answer           AI 回答
data.conversation_id  Dify 会话 ID
data.message_id       Dify 消息 ID
```

继续同一会话时，将上一次的 `data.conversation_id` 放入下一次请求的 `conversationId`。

### 查询历史消息

```http
GET /api/chatflow/messages?conversationId=<会话ID>&limit=100
token: <登录Token>
```

`limit` 必须在 1 到 100 之间，`firstId` 是可选分页参数。

## Workflow 中转

```http
POST /api/workflow/run
Content-Type: application/json
token: <登录Token>
```

远程文档示例：

```json
{
  "inputs": {
    "doc": {
      "transfer_method": "remote_url",
      "url": "https://example.com/document.pdf",
      "type": "document"
    },
    "plan": "根据文档生成教学设计"
  }
}
```

`doc` 是当前 Workflow 的必填输入，远程 URL 必须能被 Dify 服务器访问。`plan` 可选，最大长度 4090。

本地文件必须先上传到 Dify，再传入文件 ID：

```json
{
  "inputs": {
    "doc": {
      "transfer_method": "local_file",
      "upload_file_id": "<Dify文件ID>",
      "type": "document"
    },
    "plan": "根据文档生成教学设计"
  }
}
```

浏览器前端不得直接持有 Dify API Key。当前前端若要上传本地文件，应先补充专用的后端文件上传中转接口；现有 Workflow 运行接口只接收 JSON。

## AI 任务链路与接口

新 AI 主链路统一走 `/api/ai/**`，请求头携带 `token`，响应结构为 `{status,msg,requestId,data}`。前端不直接访问 Dify，由后端 `DifyGateway` 中转并将 Dify 事件转换为 [LingXiEvent v1](docs/接口与协议/LingXiEvent协议-v1.md) 后经 SSE 推送。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/ai/tasks` | 创建任务（`taskType=CHATFLOW/WORKFLOW`，可带 `conversationId` 继续会话） |
| GET | `/api/ai/tasks/{taskId}` | 任务快照（状态、结果、子任务） |
| GET | `/api/ai/tasks/{taskId}/events` | 订阅 SSE，支持 `Last-Event-ID: {taskId}:{sequence}` 断线续传 |
| POST | `/api/ai/tasks/{taskId}/stop` | 停止运行中任务 |
| POST | `/api/ai/tasks/{taskId}/subtasks/{subtaskId}/retry` | 重试失败子任务 |
| GET | `/api/ai/tasks/{taskId}/feedback` | 个性化反馈（薄弱点 + 推荐练习；优先消费 `validation_finished`，否则本地规则） |
| GET/PATCH/DELETE | `/api/ai/conversations...` | 会话列表、消息历史、重命名、软删除、继续对话 |
| GET | `/api/ai/model-calls`、`/api/ai/model-calls/export` | 模型调用日志查询与 CSV/JSON 导出（教师） |
| GET | `/api/ai/model-calls/usage-by-class` | 班级/学生用量聚合（教师） |
| GET/POST | `/api/ai/configs` | Dify 应用配置版本管理（教师） |
| POST/DELETE | `/api/ai/privacy/*` | 保留期清理、按会话/任务删除 |
| POST | `/api/ai/upload` | 本地文件上传 |

相关环境变量（`.env`，均有默认值）：`AI_SSE_TIMEOUT_MS`、`AI_SSE_HEARTBEAT_MS`、`AI_TASK_EXECUTOR_*`、`AI_TASK_RECOVERY_ENABLED`、`AI_SUBTASK_MAX_RETRIES`、`AI_MODEL_LOG_EXECUTOR_*`、`AI_PRIVACY_RETENTION_DAYS`、`AI_PRIVACY_RATE_LIMIT_PER_MINUTE`、`AI_AGENT_ROUTE_*`、`AI_GRADING_ENABLED`。

本地一键演示的启动顺序、账号与验收清单见 [本地演示启动说明](docs/部署与联调/本地演示启动说明.md)。

## 运行测试

```powershell
mvn -o clean test
```

测试为纯 JUnit 5 + Mockito（不依赖 MySQL/Redis/Dify）。当前为 201 项通过、0 失败、1 跳过。

## 测试脚本

测试 Chatflow：

```powershell
.\test-dify-chatflow.ps1 -Mode Backend -Query "1+1=?"
```

测试 Workflow：

```powershell
.\test-dify-workflow.ps1 `
  -Mode Backend `
  -DocPath ".\README.md" `
  -Plan "根据文档生成教学设计"
```

未显式传入登录 Token 时，脚本会创建短期 Redis 测试 Token，并在结束后删除。

## 相关文档

- [完整文档索引](docs/README.md)
- [当前基线与协作分工](docs/当前基线与协作分工.md)
- [本地演示启动说明](docs/部署与联调/本地演示启动说明.md)
- [AI 任务接口契约 v1](docs/接口与协议/AI任务接口契约-v1.md)
- [LingXiEvent 协议 v1](docs/接口与协议/LingXiEvent协议-v1.md)
- [隐私保护说明](docs/交付材料/报告与PPT/隐私保护说明.md)
- [后端部署方案](docs/部署与联调/后端部署方案.md)
- [Dify 连接测试](docs/部署与联调/dify连接测试.md)
- [更新日志（2026-09）](docs/交接与记录/更新日志（2026-09）.md)
- [更新总结](docs/交接与记录/更新总结.md)
- `.env.example`

## 安全说明

- 不要提交 `.env`、数据库密码、短信密钥或 Dify API Key。
- 不要在 README、Apifox 或截图中发布真实登录 Token。
- 数据库管理账号只用于人工维护，不应提供给前端或写入前端配置。
- 生产环境应限制 MySQL、Redis 和后端端口的访问来源。
