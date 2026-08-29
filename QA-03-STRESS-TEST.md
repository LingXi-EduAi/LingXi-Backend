# QA-03 压力测试报告与执行清单

## 测试目标
验证高用户并发场景下系统的稳定性与性能表现，覆盖三大核心链路：
1. **SSE 推送高并发** - 多客户端同时订阅任务事件流
2. **连接中断与重连** - 模拟网络抖动，验证 Last-Event-ID 机制的正确性
3. **大文件上传** - 50MB 文件并发上传，验证吞吐与限流

---

## 测试环境
| 项目 | 规格 |
|------|------|
| 后端地址 | `http://localhost:5678` (或配置的部署地址) |
| JDK | Java 8+ |
| 构建工具 | Maven 3.6+ |
| 依赖 | okhttp3, jackson-databind (已在测试依赖中) |

---

## 测试用例清单

### TC-01: SSE 并发连接压测 (`SseConcurrentStressTest`)
| 参数 | 默认值 | 说明 |
|------|--------|------|
| 并发连接数 | 50 | `-Dstress.connections=50` |
| 测试时长 | 60秒 | `-Dstress.duration=60` |
| 目标 taskId | 必填 | `-Dstress.taskId=xxx` |
| 认证 token | 必填 | `-Dstress.token=xxx` |

**验收标准**：
- [ ] 连接建立成功率 ≥ 95%
- [ ] 无连接泄漏（测试结束后所有连接正常关闭）
- [ ] 事件吞吐率满足业务预期（如 ≥ 100 events/sec 总计）
- [ ] 连接时长 P99 接近测试时长（说明连接稳定未被意外切断）
- [ ] 错误计数为 0

---

### TC-02: SSE 断线重连压测 (`SseReconnectionStressTest`)
| 参数 | 默认值 | 说明 |
|------|--------|------|
| 并发客户端 | 30 | `-Dstress.reconnect.count=30` |
| 重连周期数 | 5 | `-Dstress.reconnect.cycles=5` |
| 每周期目标事件数 | 10 | `-Dstress.reconnect.eventsPerCycle=10` |
| 周期间隔 | 2000ms | `-Dstress.reconnect.gapMs=2000` |

**验收标准**：
- [ ] 100% 重连成功（无因重连导致的连接失败）
- [ ] Last-Event-ID 机制生效：重连后不丢失事件、不重复接收已收事件
- [ ] 事件顺序保持正确
- [ ] 重连间隙内产生的事件在重连后正确补发

---

### TC-03: 大文件上传并发压测 (`FileUploadStressTest`)
| 参数 | 默认值 | 说明 |
|------|--------|------|
| 并发上传数 | 20 | `-Dstress.upload.count=20` |
| 测试文件 | 必填 | `-Dstress.upload.file=/path/to/file.pdf` (建议 10-50MB) |

**验收标准**：
- [ ] 100% 上传成功（无因并发导致的上传失败）
- [ ] 平均上传耗时在合理范围（如 50MB 文件 < 30 秒）
- [ ] 吞吐率满足带宽预期（无明显性能抖动）
- [ ] 服务端内存/CPU 无异常飙升
- [ ] 文件完整性校验通过（上传后可正常下载/访问）

---

## 运行步骤

### 1. 准备测试数据
```bash
# 1. 启动后端
cd LingXi-Backend
mvn spring-boot:run

# 2. 登录获取 token（通过前端登录或直接调用 /api/auth/login）
# 记下返回的 token

# 3. 创建一个长时间运行的 AI 任务获取 taskId
# 可以通过前端工作台创建，或直接调用 POST /api/ai/task/stream
# 记下 taskId

# 4. 准备大文件（用于 TC-03）
# 例如生成 20MB 测试文件：
# dd if=/dev/urandom of=test_20mb.bin bs=1M count=20
```

### 2. 运行压测
```bash
cd LingXi-Backend

# TC-01: SSE 并发
mvn test -Dtest=SseConcurrentStressTest -DfailIfNoTests=false \
  -Dstress.token=<YOUR_TOKEN> -Dstress.taskId=<YOUR_TASK_ID> \
  -Dstress.connections=50 -Dstress.duration=60

# TC-02: 断线重连
mvn test -Dtest=SseReconnectionStressTest -DfailIfNoTests=false \
  -Dstress.token=<YOUR_TOKEN> -Dstress.taskId=<YOUR_TASK_ID> \
  -Dstress.reconnect.count=30 -Dstress.reconnect.cycles=5

# TC-03: 大文件上传
mvn test -Dtest=FileUploadStressTest -DfailIfNoTests=false \
  -Dstress.token=<YOUR_TOKEN> \
  -Dstress.upload.file=/path/to/test_20mb.bin \
  -Dstress.upload.count=20
```

### 3. 结果记录
每次运行后将控制台输出保存为文件：
```bash
mvn test -Dtest=SseConcurrentStressTest ... > sse-stress-$(date +%Y%m%d-%H%M%S).log 2>&1
```

---

## 性能基线（参考值，需根据实际环境调整）

| 指标 | 目标值 | 备注 |
|------|--------|------|
| SSE 连接建立延迟 P99 | < 2 秒 | 含 TLS 握手 |
| SSE 事件端到端延迟 | < 500 ms | 从 Dify 产生到客户端收到 |
| 50MB 文件上传耗时 | < 30 秒 | 千兆内网环境 |
| 并发上传吞吐 | > 20 MB/s 总计 | 取决于存储后端 |
| 重连恢复时间 | < 3 秒 | 含 TCP 重连 + SSE 协商 |

---

## 常见问题排查

| 现象 | 可能原因 | 排查建议 |
|------|----------|----------|
| 连接成功率低 | 后端 SSE 连接数限制 / Tomcat maxConnections | 检查 `server.tomcat.max-connections`、Nginx 限制 |
| 重连后收到重复事件 | Last-Event-ID 记录/比对逻辑缺陷 | 检查 `AiTaskControlServiceImpl.subscribe()` 的 lastEventId 处理 |
| 大文件上传超时 | Nginx `client_max_body_size` / Spring `max-file-size` | 确认均配置 ≥ 50MB |
| 上传并发时内存飙升 | Multipart 解析缓冲未释放 | 检查 `FileUpload.fileUp()` 是否流式写入 |
| 事件吞吐不达标 | Dify 响应慢 / 事件广播阻塞 | 检查 `TaskEventBroadcaster` 与 `OrderedEventBuffer` |

---

## 通过判定
**全部三个测试用例 PASS 视为 QA-03 通过。**

如有单项 FAIL，需定位根因修复后回归。

---

## 维护记录
| 日期 | 版本 | 变更 | 执行人 |
|------|------|------|--------|
| 2026-08-29 | 1.0 | 初版创建，包含三大压测类与执行清单 | Sisyphus |