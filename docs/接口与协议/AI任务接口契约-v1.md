# AI 任务接口契约 v1

本文件记录统一 AI 任务接口。BE-02 的 `POST /api/ai/task` 阻塞式接口保持兼容；BE-06 在不改变该响应契约的前提下增加 `POST /api/ai/task/stream` 流式扩展。

## 1. 接口信息

- 请求方法：`POST`
- 接口路径：`/api/ai/task`
- 请求格式：`application/json`
- 登录要求：请求头必须携带灵犀登录接口返回的 `token`

后端从登录 Token 中取得当前用户 ID，并将其作为 Dify `user`。前端不能传入或覆盖 Dify user，也不能接触 Dify API Key。

## 2. 请求体

|字段|类型|必需|说明|
|---|---|---|---|
|query|string|是|用户问题，不能为空|
|conversationId|string|否|首次对话为空；继续对话时传上次响应中的会话 ID|

示例：

```json
{
  "query": "请解释一元二次方程的求根公式",
  "conversationId": ""
}
```

## 3. 成功响应

接口沿用现有 `ResultConstant` 响应结构：

```json
{
  "status": 200,
  "msg": "success",
  "data": {
    "answer": "一元二次方程的求根公式是……",
    "conversationId": "dify-conversation-id",
    "messageId": "dify-message-id",
    "elapsedMs": 1250
  }
}
```

|字段|类型|说明|
|---|---|---|
|answer|string|Chatflow 返回的回答|
|conversationId|string|Dify 会话 ID，继续对话时回传|
|messageId|string|Dify 消息 ID|
|elapsedMs|long|本次后端调用耗时，单位毫秒|

## 4. 错误响应

|场景|业务状态|消息示例|
|---|---:|---|
|query 为空|500|`query 不能为空`|
|Token 缺失或失效|1000|`参数缺少 token 值` 或 `登录过期，请重新登录`|
|Dify 调用失败|300|Gateway 返回的安全错误信息|
|后端内部异常|300|`AI 任务调用失败`|

## 5. 当前调用链

```text
Frontend
  -> POST /api/ai/task
  -> AiTaskController
  -> AiTaskService
  -> DifyGateway
  -> Dify Chatflow
```

`AiAgentRouter` 当前统一路由到 Chatflow，只有在后续需求明确增加学科 Agent 后才扩展路由规则。

## 6. BE-06 流式扩展

### 接口信息

- 请求方法：`POST`
- 接口路径：`/api/ai/task/stream`
- 请求头：必须携带灵犀登录接口返回的 `token`，并建议携带 `Accept: text/event-stream`
- 请求体：与阻塞接口相同，使用 `query` 和可选的 `conversationId`
- 成功响应：`200 OK`、`Content-Type: text/event-stream`

请求示例：

```http
POST /api/ai/task/stream
Accept: text/event-stream
Content-Type: application/json
token: <灵犀登录 Token>
```

```json
{
  "query": "1+1等于多少？",
  "conversationId": ""
}
```

响应是由多条 SSE 事件组成的流。每条事件的 `id` 等于事件体的 `eventId`，`event` 等于事件体的 `eventType`，JSON 数据结构见 [LingXiEvent协议-v1.md](LingXiEvent协议-v1.md)。前端应监听 `task_started`、`answer_delta`、`task_finished` 和 `task_error`，并可根据 `node_progress` 更新节点轨迹。

示例：

```text
id: task-id:1
event: task_started
data: {"eventId":"task-id:1","sequence":1,"eventType":"task_started",...}

id: task-id:2
event: answer_delta
data: {"eventId":"task-id:2","sequence":2,"eventType":"answer_delta",...}
```

后端会每 15 秒发送一次 SSE 注释心跳：

```text
:heartbeat
```

心跳不是 LingXiEvent，不占用 `sequence`。默认连接超时为 10 分钟，可通过 `AI_SSE_TIMEOUT_MS` 和 `AI_SSE_HEARTBEAT_MS` 配置。

该接口是 POST 请求，浏览器原生 `EventSource` 只支持 GET，因此前端必须使用 `fetch` 配合 `ReadableStream` 读取 SSE，并在请求头中传递 Token。部署 Nginx 或其他反向代理时必须关闭响应缓冲，并将代理读取超时设置为大于 `AI_SSE_TIMEOUT_MS`，否则事件可能被代理攒成一次性响应或提前断开。后端已经返回 `Cache-Control: no-cache` 和 `X-Accel-Buffering: no`。

## 7. 兼容边界

- 现有 `/api/chatflow/**`、`/api/workflow/**` 和旧 `/api/**` 接口继续保留。
- 本版本不创建数据库任务记录，不提供任务状态查询。
- BE-06 只提供当前请求内的直接 SSE 代理，不提供任务持久化、停止、重试或断线恢复。
- 流式接口生成的 `taskId` 仅在当前连接期间有效；`GET /api/ai/tasks/{taskId}/events` 等可恢复任务接口待 BE-07 至 BE-09 完成后再发布。
- Workflow 继续使用现有 `/api/workflow/run`，不合并到本接口。
