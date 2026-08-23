# DEMO-01-2 — 灌入丰富的演示会话数据

- **板块**：A8 演示数据（Part A 初赛）
- **波次**：A4
- **优先级**：MUST（视频需要）
- **前置依赖**：BE-10-3、BE-11-1、BE-13-2
- **工作量**：1 人天

## 目标

一条预跑完成的完整会话（任务+子任务+事件+消息+证据+模型日志），视频可展示轨迹/证据/会话视图，不冒实时 AI 风险。

## 输入

- 真实运行时形态（事件序列、表结构）
- 6 张 AI 表

## 实施内容

1. 新增 `LingXi-Backend/sql/seed/demo_ai_data.sql`：为 `student114514` 插入一条完整演示会话，6 张 AI 表全部填充，状态/序列真实，含 ≥1 条 `retrieval_finished`/`validation_finished` 事件；固定 UUID、**幂等**（可安全重跑）。
2. 可选 `util/AiDemoSeeder.java`（dev-only 触发器）。

## 验收标准

- 种子可重复执行。
- 截图/视频可渲染完整轨迹+证据+会话+日志视图。

## 工作区域

- 新增 `LingXi-Backend/sql/seed/demo_ai_data.sql` + 可选 `util/AiDemoSeeder.java`

## 约束

- 不动既有 seed/业务表。
