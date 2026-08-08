# LingXiEvent 协议 v1

更新日期：2026-08-09

本协议由 BE-05 冻结。前端只处理 LingXiEvent，不依赖 Dify 原始事件名称和字段。v1 已发布字段不得改变类型或语义；新增字段必须保持向后兼容，不兼容变更必须升级 `payloadVersion`。

## 1. 公共字段

|字段|类型|必需|说明|
|---|---|---|---|
|eventId|string|是|事件唯一标识，当前格式为 `{taskId}:{sequence}`|
|sequence|long|是|单个任务内从 1 开始单调递增|
|eventType|string|是|事件类型，见下表|
|taskId|string|是|灵犀后端生成的任务 ID，不使用 Dify ID 作为业务主键|
|conversationId|string|是|会话 ID；Dify 尚未返回时为空字符串|
|occurredAt|string|是|后端产生事件的 UTC ISO-8601 时间|
|status|string|是|任务状态：`CREATED`、`RUNNING`、`SUCCEEDED`、`FAILED`、`STOPPED` 或 `PARTIAL_SUCCESS`|
|payloadVersion|integer|是|当前固定为 `1`|
|payload|object|是|不同事件的业务数据；没有数据时返回空对象|

## 2. v1 事件类型

|eventType|用途|payload 必需字段|payload 可选字段|
|---|---|---|---|
|task_started|任务开始|questionSummary|无|
|task_decomposed|任务拆解完成|subtasks|无|
|agent_assigned|Agent 分配完成|subtaskId、agentType、goal|无|
|node_progress|节点开始或结束|nodeId、nodeName、nodeType、nodeStatus|elapsedMs、difyTaskId|
|retrieval_finished|知识检索完成|sources|无|
|validation_finished|结果校验完成|checks、conclusion|suggestions|
|answer_delta|答案文本增量|delta|messageId、difyTaskId|
|task_finished|任务成功结束|finishReason|messageId、difyTaskId、usage|
|task_error|任务失败|code、message、retryable|无|

可选字段没有值时直接省略，不返回 `null`。`payload` 可新增可选字段，但不得删除 v1 必需字段或改变现有字段类型。

## 3. 示例事件

任务开始：

```json
{
  "eventId": "3a7d4a82-3e61-4d11-a0eb-52b98fb38737:1",
  "sequence": 1,
  "eventType": "task_started",
  "taskId": "3a7d4a82-3e61-4d11-a0eb-52b98fb38737",
  "conversationId": "",
  "occurredAt": "2026-08-09T08:00:00Z",
  "status": "RUNNING",
  "payloadVersion": 1,
  "payload": {
    "questionSummary": "请解释一元二次方程的求根公式"
  }
}
```

答案增量：

```json
{
  "eventId": "3a7d4a82-3e61-4d11-a0eb-52b98fb38737:2",
  "sequence": 2,
  "eventType": "answer_delta",
  "taskId": "3a7d4a82-3e61-4d11-a0eb-52b98fb38737",
  "conversationId": "dify-conversation-id",
  "occurredAt": "2026-08-09T08:00:01Z",
  "status": "RUNNING",
  "payloadVersion": 1,
  "payload": {
    "delta": "一元二次方程的求根公式是",
    "messageId": "dify-message-id",
    "difyTaskId": "dify-task-id"
  }
}
```

任务失败：

```json
{
  "eventId": "3a7d4a82-3e61-4d11-a0eb-52b98fb38737:3",
  "sequence": 3,
  "eventType": "task_error",
  "taskId": "3a7d4a82-3e61-4d11-a0eb-52b98fb38737",
  "conversationId": "dify-conversation-id",
  "occurredAt": "2026-08-09T08:00:02Z",
  "status": "FAILED",
  "payloadVersion": 1,
  "payload": {
    "code": "UPSTREAM_ERROR",
    "message": "AI 任务执行失败",
    "retryable": true
  }
}
```

## 4. BE-05 范围

BE-05 只冻结事件公共结构、事件类型、payload 字段和示例，不规定具体 HTTP 路径、SSE 连接方式、Dify 事件映射、事件持久化或断线续传实现。这些能力由后续 BE 任务按本协议输出事件。
