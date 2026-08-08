# AI 任务接口契约 v1

本文件记录 BE-02 当前已经实现的统一 AI 任务接口。接口使用阻塞模式调用 Chatflow，后续异步任务、SSE、停止和重试能力分别由对应 BE 任务扩展，不在本版本提前定义。

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

## 6. 兼容边界

- 现有 `/api/chatflow/**`、`/api/workflow/**` 和旧 `/api/**` 接口继续保留。
- 本版本不创建数据库任务记录，不提供任务状态查询。
- 本版本不提供 SSE、停止、重试或断线恢复。
- Workflow 继续使用现有 `/api/workflow/run`，不合并到本接口。
