# 变更日志

## 2026-08-21 — QA-00：修复后端测试环境

### 改了啥

| 文件 | 改动 |
|------|------|
| `pom.xml` | 新增 `maven-surefire-plugin 2.22.2`（此前未声明版本，默认不支持 JUnit 5，导致 `mvn test` 跑 0 个测试） |
| `src/test/java/com/lxe/lx/LxApplicationTests.java` | 全上下文加载测试（`contextLoads`）需 MySQL/Redis/环境变量，本地无法满足，按任务卡约定加 `@Disabled` 跳过并注释说明策略；云端/CI 有环境时恢复 |

### 效果

- `mvn test` 从"假绿"（Tests run: 0）变为**真实跑通 25 用例 / 9 测试类**（0 失败、0 错误），成为后续板块的稳定质量闸门。

### 有影响吗

- **没事**：`mvn test`、`mvn spring-boot:run` 行为不变，`skip=true` 打包策略未动。
- **注意**：`LxApplicationTests` 在无外部环境的机器上会跳过；具备 MySQL/Redis 的部署环境可移除 `@Disabled` 恢复。