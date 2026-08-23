# 变更日志

## 2026-07-23 — SEC-01 轮换密钥

### 改了啥

| 文件 | 改动 |
|------|------|
| `AGENTS.md` | 去掉 Dify/MySQL/元景万悟 明文密码，改成"凭据走环境变量" |
| `前端/src/config/api.js` | 删掉硬编码 `'https://cloud.dify.ai'` 回落地址 |
| `前端/src/config/env.js` | 删掉开发环境里的 `'https://cloud.dify.ai'` 默认值 |
| `前端/src/views/student/SelfTestAssistant.vue` | 删掉 `'http://68.183.234.61'` 硬编码 IP |
| `后端/application.properties` | 补了第 62 行缺失的 `#` |
| `前端/.env.development`（新增） | 团队统一的云环境地址（.env 里放 Dify Key，已忽略） |

### 有影响吗

- **没事**：配了环境变量的人
- **可能有问题**：之前没配 `VUE_APP_AI_API`、靠自动回落地址跑的人，AI 页面会打不开
