package com.lxe.lx;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 全上下文加载冒烟测试（验证 Spring 容器可完整启动）。
 *
 * <p>策略（QA-00-2）：本地/CI 无 MySQL、Redis 及环境变量（.env 未加载）时，
 * Spring 上下文无法解析 ${API_BASE_URL} 等占位符并连接数据库，必然失败。
 * 按任务卡约定「跳过或 profile 隔离」处理，暂以 @Disabled 跳过。
 * 在具备完整环境（云端部署 / CI secrets 注入）的流水线中应移除 @Disabled 恢复执行。</p>
 */
@SpringBootTest
@Disabled("需要 MySQL/Redis/环境变量；本地无环境按 QA-00-2 约定跳过，云端/CI 恢复")
class LxApplicationTests {

    @Test
    void contextLoads() {
    }

}
