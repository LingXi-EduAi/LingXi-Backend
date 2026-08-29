package com.lxe.lx.stress;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * QA-03 压力测试：SSE 并发连接压测
 *
 * 运行前提：
 *   - 后端已启动（默认 http://localhost:5678）
 *   - 已存在有效用户 token（需手动登录获取或配置测试账号）
 *   - 已有至少一个 RUNNING 状态的 taskId 用于订阅
 *
 * 运行方式：
 *   mvn test -Dtest=SseConcurrentStressTest -DfailIfNoTests=false
 *   或在 IDE 中直接运行 main 方法
 *
 * 指标输出：
 *   - 连接建立成功率
 *   - 事件接收吞吐（events/sec）
 *   - 连接存活时长分布
 *   - 错误分类统计
 */
public class SseConcurrentStressTest {

    // ===== 配置区 =====
    private static final String BASE_URL = System.getProperty("stress.baseUrl", "http://localhost:5678");
    private static final String AUTH_TOKEN = System.getProperty("stress.token", ""); // 需填入有效 token
    private static final String TASK_ID = System.getProperty("stress.taskId", "");   // 需填入有效 taskId

    // 压测参数
    private static final int CONCURRENT_CONNECTIONS = Integer.parseInt(System.getProperty("stress.connections", "50"));
    private static final int TEST_DURATION_SECONDS = Integer.parseInt(System.getProperty("stress.duration", "60"));
    private static final int CONNECT_TIMEOUT_SEC = 10;
    private static final int READ_TIMEOUT_SEC = TEST_DURATION_SECONDS + 30;
    // ===================

    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_SEC))
            .readTimeout(Duration.ofSeconds(READ_TIMEOUT_SEC))
            .writeTimeout(Duration.ofSeconds(10))
            .build();

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // 统计指标
    private static final LongAdder connectionsEstablished = new LongAdder();
    private static final LongAdder connectionsFailed = new LongAdder();
    private static final LongAdder totalEventsReceived = new LongAdder();
    private static final LongAdder totalErrors = new LongAdder();
    private static final LongAdder reconnectAttempts = new LongAdder();
    private static final List<Long> connectionDurationsMs = new CopyOnWriteArrayList<>();

    public static void main(String[] args) throws Exception {
        if (AUTH_TOKEN.isEmpty() || TASK_ID.isEmpty()) {
            System.err.println("ERROR: 必须提供 -Dstress.token=<token> -Dstress.taskId=<taskId>");
            System.err.println("示例: mvn test -Dtest=SseConcurrentStressTest -Dstress.token=xxx -Dstress.taskId=yyy");
            System.exit(1);
        }

        System.out.println("========================================");
        System.out.println("QA-03 SSE 并发压力测试启动");
        System.out.println("BASE_URL: " + BASE_URL);
        System.out.println("CONCURRENT_CONNECTIONS: " + CONCURRENT_CONNECTIONS);
        System.out.println("TEST_DURATION_SECONDS: " + TEST_DURATION_SECONDS);
        System.out.println("TASK_ID: " + TASK_ID);
        System.out.println("========================================");

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_CONNECTIONS);
        CountDownLatch startLatch = new CountDownLatch(CONCURRENT_CONNECTIONS);
        CountDownLatch doneLatch = new CountDownLatch(CONCURRENT_CONNECTIONS);

        Instant testStart = Instant.now();

        // 启动并发连接
        for (int i = 0; i < CONCURRENT_CONNECTIONS; i++) {
            final int connIndex = i;
            executor.submit(() -> {
                startLatch.countDown();
                try { startLatch.await(); } catch (InterruptedException ignored) {}
                runSingleConnection(connIndex, testStart);
                doneLatch.countDown();
            });
        }

        // 等待测试时长
        Thread.sleep(Duration.ofSeconds(TEST_DURATION_SECONDS).toMillis());

        // 优雅关闭
        executor.shutdown();
        try { executor.awaitTermination(30, TimeUnit.SECONDS); }
        catch (InterruptedException e) { executor.shutdownNow(); }

        Instant testEnd = Instant.now();
        printReport(Duration.between(testStart, testEnd));
    }

    private static void runSingleConnection(int connIndex, Instant testStart) {
        Instant connStart = Instant.now();
        String url = BASE_URL + "/api/ai/tasks/" + TASK_ID + "/events";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + AUTH_TOKEN)
                .addHeader("Accept", "text/event-stream")
                .addHeader("Cache-Control", "no-cache")
                .build();

        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                connectionsFailed.increment();
                totalErrors.increment();
                System.err.printf("[Conn-%d] 连接失败: HTTP %d %s%n", connIndex, response.code(), response.message());
                return;
            }

            connectionsEstablished.increment();
            System.out.printf("[Conn-%d] SSE 连接建立成功%n", connIndex);

            ResponseBody body = response.body();
            if (body == null) {
                System.err.printf("[Conn-%d] 响应体为空%n", connIndex);
                return;
            }

            long eventsThisConn = 0;

            // SSE 流式读取
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(body.byteStream()))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isEmpty() || line.startsWith(":")) continue; // 心跳/注释
                    if (line.startsWith("data:")) {
                        String data = line.substring(5).trim();
                        if (!data.isEmpty()) {
                            eventsThisConn++;
                            totalEventsReceived.increment();
                        }
                    }
                    // 可选：解析 eventType 等字段做更细粒度统计
                }
            }

            long durationMs = Duration.between(connStart, Instant.now()).toMillis();
            connectionDurationsMs.add(durationMs);
            System.out.printf("[Conn-%d] 连接结束，持续 %d ms，收到 %d 事件%n", connIndex, durationMs, eventsThisConn);

        } catch (IOException e) {
            connectionsFailed.increment();
            totalErrors.increment();
            System.err.printf("[Conn-%d] 连接异常: %s%n", connIndex, e.getMessage());
        }
    }

    private static void printReport(Duration totalDuration) {
        System.out.println("\n========================================");
        System.out.println("QA-03 SSE 压测报告");
        System.out.println("========================================");
        System.out.printf("测试总时长: %d 秒%n", totalDuration.getSeconds());
        System.out.printf("目标并发数: %d%n", CONCURRENT_CONNECTIONS);
        System.out.printf("连接成功: %d%n", connectionsEstablished.sum());
        System.out.printf("连接失败: %d%n", connectionsFailed.sum());
        System.out.printf("总接收事件数: %d%n", totalEventsReceived.sum());
        System.out.printf("总错误数: %d%n", totalErrors.sum());

        if (connectionsEstablished.sum() > 0) {
            double throughput = (double) totalEventsReceived.sum() / totalDuration.getSeconds();
            System.out.printf("事件吞吐率: %.2f events/sec%n", throughput);

            // 连接时长统计
            List<Long> durations = new ArrayList<>(connectionDurationsMs);
            durations.sort(Long::compareTo);
            long p50 = durations.get(durations.size() / 2);
            long p95 = durations.get((int) (durations.size() * 0.95));
            long p99 = durations.get((int) (durations.size() * 0.99));
            System.out.printf("连接时长 P50: %d ms, P95: %d ms, P99: %d ms%n", p50, p95, p99);
        }

        double successRate = (double) connectionsEstablished.sum() / CONCURRENT_CONNECTIONS * 100;
        System.out.printf("连接成功率: %.1f%%%n", successRate);

        // 判定标准（可根据实际调整）
        boolean pass = successRate >= 95.0 && totalErrors.sum() == 0;
        System.out.println(pass ? "\n>>> 结果: PASS" : "\n>>> 结果: FAIL");
        System.out.println("========================================");
    }
}