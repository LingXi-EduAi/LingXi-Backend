# DEMO-01-1 — 云端验证核心闭环端到端

- **板块**：A2 云端闭环验证（Part A 初赛）
- **波次**：A0
- **优先级**：MUST
- **前置依赖**：无（用既有 BE-02/BE-06）
- **工作量**：0.5 人天

## 目标

在 `120.26.144.127:15678` 上证明 `登录(student114514) → POST /api/ai/task/stream → LingXiEvent SSE → Dify 回答` 可用，再在其上构建 UI。

## 输入

- 云端后端 `120.26.144.127:15678`
- 测试账号 `student114514/123456`
- 既有 BE-02（任务接口）/ BE-06（SSE 流式代理）

## 实施内容

1. 登录获取 token。
2. 调用 `POST /api/ai/task/stream`，订阅 SSE。
3. 记录事件流，确认 `task_started`、`answer_delta`、`task_finished` 全部出现且 `payloadVersion=1`。
4. 任何阻塞性失败：修复或记录（不改仓库；发现真实缺陷则另开修复任务）。

## 验收标准

- 录制一次完整运行，所有预期事件类型出现。
- 运行素材存档供视频/报告引用。

## 工作区域

- 只读验证 + 服务器 `.env`/`application.properties`（不改仓库）

## 约束

- 凭据只走环境变量，不落仓库。
