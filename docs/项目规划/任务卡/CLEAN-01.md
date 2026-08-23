# CLEAN-01 — 退役旧版 Dify 兼容层

- **板块**：B8 退役旧版 Dify 兼容层（Part B 决赛）
- **波次**：B3
- **优先级**：Part B
- **前置依赖**：A7、B4、A6
- **工作量**：0.5 人天

## 目标

单一 Dify 入口，旧端点返回 404。

## 输入

- `ApiController` 7 个旧端点（`/api/chatMessage`、`/api/conversations`、`/api/deleteConversation`、`/api/renameConversation`、`/api/messages`、`/api/filesUpload`、`/api/test`）+ `ApiService` 旧方法 + 注释掉的 `audioToText`——前端已全部迁移到新入口后

## 实施内容

1. 删除旧端点/旧方法/注释代码。
2. grep 验证前端零调用方。

## 验收标准

- 旧端点返回 404；`mvn test` 绿。

## 工作区域

- `ApiController`、`ApiService`（删除）

## 约束

- 确认前端零调用后删除。
- Java 8。
