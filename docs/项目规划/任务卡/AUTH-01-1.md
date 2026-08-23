# AUTH-01-1 — 审计并强制所有 `/api/ai/*` 端点的鉴权与用户隔离

- **板块**：A1 安全基线（Part A 初赛）
- **波次**：A0
- **优先级**：MUST
- **前置依赖**：无（新端点上线前的审计闸门）
- **工作量**：0.5 人天

## 目标

演示前加固公网面。当前 AI 端点只有 `@Login`，无角色校验、无限流；前端 `guards.js` 路由守卫是死代码（从未 import）；token 经 URL 查询参数传递。

## 输入

- 后端 `@Login` 注解 + `config/AuthorizationInterceptor.java`
- 前端 `src/router/guards.js`、`src/utils/api.js`、`src/utils/request.js`、`src/main.js`、`src/router/index.js`

## 实施内容

1. 审计每个 `/api/ai/**` 端点（含新增 BE-11/BE-13），要求有效 token，并按 `TokenEntity.getId()` 过滤查询；跨用户访问任务/会话 → 404/403。
2. Token 移出 URL 查询参数：`api.js`/`request.js` 不再追加 `token=`，改请求头（契约：token 头）。
3. 接线 `guards.js`（在 `main.js` 或 `router/index.js` import），`/teacher/*`、`/student/*` 按角色守卫。
4. 增加演示期 token 刷新/延长选项（风险 #5 缓解）。

## 验收标准

- URL 中无 `token=`。
- grep 无 `@/utils/request` 残留调用（除 `api.js` 与待退役的 `TeachingAssistant.vue`）。
- 跨用户访问返回 404/403。
- 新路由上线前角色守卫已生效。

## 工作区域

- 后端 `controller/**`、`config/AuthorizationInterceptor.java`（仅当证明有缺口）、`src/test/java/**`
- 前端 `src/main.js`、`src/router/index.js`、`src/router/guards.js`、`src/utils/api.js`、`src/utils/request.js`

## 约束

- 不加 Spring Security（自定义鉴权体系）。
- 凭据只走环境变量。
