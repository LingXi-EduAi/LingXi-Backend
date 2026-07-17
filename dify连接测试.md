# Dify 连接测试

**更新日期：2026-07-17**

## 一、测试脚本

后端根目录提供两个 PowerShell 脚本：

| 脚本 | 用途 |
|------|------|
| `test-dify-chatflow.ps1` | 测试 Chatflow 直连和后端中转 |
| `test-dify-workflow.ps1` | 测试 Workflow 直连和后端中转 |

脚本会自动读取同目录的 `.env`，不会打印 API Key。

默认 `Mode` 为 `All`，会依次测试：

1. 本机直接请求 Dify。
2. 本机请求灵犀后端，由后端中转到 Dify。

后端中转测试未指定真实登录 Token 时，脚本会在 Redis 中创建一个 5 分钟有效的临时 Token，并在测试结束后自动删除。

## 二、使用前准备

### 2.1 配置环境变量

在后端 `.env` 中配置：

```properties
DIFY_CHATFLOW_BASE_URL=http://120.27.159.107/v1
DIFY_CHATFLOW_API_KEY=<真实Chatflow API Key>

DIFY_WORKFLOW_BASE_URL=http://120.26.144.127:6004/v1
DIFY_WORKFLOW_API_KEY=<真实Workflow API Key>
```

Chatflow 和 Workflow 是两个独立应用，地址和 API Key 不能混用。

### 2.2 启动 Redis

```powershell
wsl redis-cli ping
```

应返回：

```text
PONG
```

### 2.3 启动后端

后端中转测试要求服务运行在：

```text
http://127.0.0.1:5678
```

修改 `.env` 后必须重启后端，正在运行的 Spring Boot 进程不会自动重新读取环境变量。

### 2.4 进入后端目录

```powershell
cd D:\LingXiAI\LingXi-Backend
```

如果 PowerShell 阻止执行脚本，可仅对当前终端临时放行：

```powershell
Set-ExecutionPolicy -Scope Process Bypass
```

## 三、Chatflow 脚本用法

### 3.1 执行完整测试

```powershell
.\test-dify-chatflow.ps1
```

### 3.2 指定测试问题

```powershell
.\test-dify-chatflow.ps1 -Query "1+1=？"
```

### 3.3 只测试 Dify 直连

```powershell
.\test-dify-chatflow.ps1 -Mode Direct -Query "1+1=？"
```

### 3.4 只测试后端中转

脚本自动创建 Redis 临时 Token：

```powershell
.\test-dify-chatflow.ps1 -Mode Backend -Query "1+1=？"
```

使用前端登录后获得的真实 Token：

```powershell
.\test-dify-chatflow.ps1 -Mode Backend -Token "<登录Token>" -Query "1+1=？"
```

### 3.5 指定后端地址

```powershell
.\test-dify-chatflow.ps1 `
    -Mode Backend `
    -BackendUrl "http://127.0.0.1:5678" `
    -Query "1+1=？"
```

### 3.6 Chatflow 参数

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `Mode` | `All` | `Direct`、`Backend` 或 `All` |
| `Query` | `1+1=?` | 发送给 Chatflow 的问题 |
| `Token` | 空 | 后端登录 Token；为空时自动创建临时 Token |
| `BackendUrl` | `http://127.0.0.1:5678` | 灵犀后端地址 |

## 四、Workflow 脚本用法

当前 Workflow 的开始节点包含：

- `doc`：必填文档文件，支持本地上传或远程 URL。
- `plan`：可选的教学设计文本。

脚本可以通过 `DocPath` 自动上传本地文档，也可以通过 `DocUrl` 传入远程文档。

### 4.1 上传本地文档并执行完整测试

```powershell
.\test-dify-workflow.ps1 `
    -DocPath ".\README.md"
```

### 4.2 上传文档并传入教学设计

```powershell
.\test-dify-workflow.ps1 `
    -DocPath "D:\资料\课程文档.docx" `
    -Plan "根据文档生成一份教学设计"
```

### 4.3 使用远程文档

```powershell
.\test-dify-workflow.ps1 `
    -DocUrl "https://example.com/course.docx" `
    -Plan "生成教学设计"
```

### 4.4 只测试 Dify 直连

```powershell
.\test-dify-workflow.ps1 `
    -Mode Direct `
    -DocPath ".\README.md"
```

### 4.5 只测试后端中转

脚本自动创建 Redis 临时 Token：

```powershell
.\test-dify-workflow.ps1 `
    -Mode Backend `
    -DocPath ".\README.md"
```

使用真实登录 Token：

```powershell
.\test-dify-workflow.ps1 `
    -Mode Backend `
    -Token "<登录Token>" `
    -DocPath "D:\资料\课程文档.docx"
```

### 4.6 指定后端地址

```powershell
.\test-dify-workflow.ps1 `
    -Mode Backend `
    -BackendUrl "http://127.0.0.1:5678" `
    -DocPath ".\README.md"
```

### 4.7 Workflow 参数

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `Mode` | `All` | `Direct`、`Backend` 或 `All` |
| `InputsJson` | `{}` | 工作流开始节点的输入 JSON |
| `DocPath` | 空 | 本地文档路径；脚本会先上传到 Dify |
| `DocUrl` | 空 | 可公开访问的远程文档 URL |
| `Plan` | 空 | 可选的教学设计文本 |
| `Token` | 空 | 后端登录 Token；为空时自动创建临时 Token |
| `BackendUrl` | `http://127.0.0.1:5678` | 灵犀后端地址 |

## 五、成功结果

Chatflow 直连成功：

```text
Dify request succeeded.
Answer: 1+1=2
```

Chatflow 后端中转成功：

```text
Backend status: 200
Backend message: success
Answer delivered through backend: 1+1=2
All selected tests passed.
```

Workflow 成功时会显示：

```text
Workflow run ID: ...
Task ID: ...
Workflow status: succeeded
Outputs:
{ ... }
```

## 六、常见错误

| 错误 | 原因或处理方式 |
|------|----------------|
| `TCP FAILED` | 检查 Dify 地址、端口、安全组和防火墙 |
| `401` | 对应应用的 API Key 错误 |
| `404` | Base URL 缺少 `/v1` 或 API 路径错误 |
| `Workflow not published` | 在 Dify 中发布对应 Chatflow 或 Workflow |
| `not_workflow_app` | 使用了非 Workflow 应用的 Key |
| `InputsJson is not valid JSON` | 检查 JSON 引号、逗号和括号 |
| 输入变量缺失 | 按工作流开始节点补充 `InputsJson` |
| `Redis is unavailable` | 在 Ubuntu 中执行 `sudo service redis-server start` |
| `参数缺少 token 值` | 启动 Redis，或通过 `-Token` 传入有效 Token |

脚本返回退出码 `0` 表示所有选定测试通过；返回退出码 `1` 表示至少一项测试失败。
