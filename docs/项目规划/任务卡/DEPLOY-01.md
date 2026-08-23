# DEPLOY-01 — 云部署 + nginx SSE 配置

- **板块**：A9 云部署（Part A 初赛）
- **波次**：A5
- **优先级**：MUST
- **前置依赖**：FE-05-1、BE-11-1
- **工作量**：1 人天

## 目标

录制前 `120.26.144.127` 可在线演示。

## 输入

- 前端构建产物（dist）、后端编译产物
- 云端 MySQL（`120.26.144.127:3306`）

## 实施内容

1. 后端在 `:15678` 带 BE-10/11/13 运行（含云端执行 `20260809_be08_ai_tables_up.sql` 迁移，若未执行过）。
2. 前端构建部署。
3. nginx 配置 `proxy_buffering off`、`X-Accel-Buffering no`、`proxy_read_timeout > AI_SSE_TIMEOUT_MS`。
4. `.env`/`.env.example` 更新（仅名称，不提交值）。
5. 部署方式：**无 fat jar**（`spring-boot-maven-plugin` skip=true）→ 按后端部署方案用 compile+classpath 启动。

## 验收标准

- 公网 URL 流式可用（SSE 不被 nginx 缓冲/截断）。

## 工作区域

- 服务器配置（nginx、systemd）、`.env.example`（仅名称）、`LingXi-Backend/` 下部署脚本（可选新增 `.ps1`/`.sh`）

## 约束

- 凭据只走环境变量，不落仓库。
