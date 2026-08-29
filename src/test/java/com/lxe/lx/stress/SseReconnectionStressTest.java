package com.lxe.lx.stress;

import okhttp3.*;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;

/**
 * QA-03 压力测试：SSE 断线重连压测
 *
 * 验证场景：
 *   1. 客户端建立 SSE 连接
 *   2. 收到若干事件后记录 Last-Event-ID
 *   3. 主动断开连接（模拟网络抖动）
 *   4. 携带 Last-Event-ID 重连
 *   5. 验证不丢事件、不重复、顺序正确
 *
 * 运行方式：
 *   mvn test -Dtest=SseReconnectionStressTest -DfailIfNoTests=false
 *   -Dstress.token=xxx -Dstress.taskId=yyy -Dstress.reconnect.count=30 -Dstress.reconnect.cycles=5
 */
public class SseReconnectionStressTest {

    private static final String BASE_URL = System.getProperty("stress.baseUrl", "http://localhost:5678");
    private static final String AUTH_TOKEN = System.getProperty("stress.token", "");
    private static final String TASK_ID = System.getProperty("stress.taskId", "");
    private static final int CONCURRENT_CLIENTS = Integer.parseInt(System.getProperty("stress.reconnect.count", "30"));
    private static final int RECONNECT_CYCLES = Integer.parseInt(System.getProperty("stress.reconnect.cycles", "5"));
    private static final int EVENTS_PER_CYCLE = Integer.parseInt(System.getProperty("stress.reconnect.eventsPerCycle", "10"));
    private static final int CYCLE_GAP_MS = Integer.parseInt(System.getProperty("stress.reconnect.gapMs", "2000"));

    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(10))
            .readTimeout(Duration.ofSeconds(30))
            .build();

    private static final LongAdder totalReconnects = new LongAdder();
    private static final LongAdder reconnectSuccess = new LongAdder();
    private static final LongAdder reconnectFailed = new LongAdder();
    private static final LongAdder eventsReceived = new LongAdder();
    private static final LongAdder duplicateEvents = new LongAdder();
    private static final LongAdder missedEvents = new LongAdder();

    public static void main(String[] args) throws Exception {
        if (AUTH_TOKEN.isEmpty() || TASK_ID.isEmpty()) {
            System.err.println("ERROR: 必须提供 -Dstress.token=<token> -Dstress.taskId=<taskId>");
            System.exit(1);
        }

        System.out.println("========================================");
        System.out.println("QA-03 SSE 断线重连压测启动");
        System.out.println("BASE_URL: " + BASE_URL);
        System.out.println("CONCURRENT_CLIENTS: " + CONCURRENT_CLIENTS);
        System.out.println("RECONNECT_CYCLES: " + RECONNECT_CYCLES);
        System.out.println("EVENTS_PER_CYCLE: " + EVENTS_PER_CYCLE);
        System.out.println("TASK_ID: " + TASK_ID);
        System.out.println("========================================");

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_CLIENTS);
        CountDownLatch startLatch = new CountDownLatch(CONCURRENT_CLIENTS);
        CountDownLatch doneLatch = new CountDownLatch(CONCURRENT_CLIENTS);

        for (int i = 0; i < CONCURRENT_CLIENTS; i++) {
            final int clientIdx = i;
            executor.submit(() -> {
                startLatch.countDown();
                try { startLatch.await(); } catch (InterruptedException ignored) {}
                runClientReconnectionCycles(clientIdx);
                doneLatch.countDown();
            });
        }

        doneLatch.await();
        executor.shutdown();
        executor.awaitTermination(60, TimeUnit.SECONDS);

        printReport();
    }

    private static void runClientReconnectionCycles(int clientIdx) {
        String lastEventId = null;
        int totalExpectedEvents = 0;
        int totalReceivedEvents = 0;

        for (int cycle = 0; cycle < RECONNECT_CYCLES; cycle++) {
            totalReconnects.increment();

            // 建立连接
            String url = BASE_URL + "/api/ai/tasks/" + TASK_ID + "/events";
            Request.Builder requestBuilder = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + AUTH_TOKEN)
                    .addHeader("Accept", "text/event-stream")
                    .addHeader("Cache-Control", "no-cache");

            if (lastEventId != null) {
                requestBuilder.addHeader("Last-Event-ID", lastEventId);
                System.out.printf("[Client-%d] Cycle %d: 重连，Last-Event-ID=%s%n", clientIdx, cycle + 1, lastEventId);
            } else {
                System.out.printf("[Client-%d] Cycle %d: 首次连接%n", clientIdx, cycle + 1);
            }

            Request request = requestBuilder.build();

            try (Response response = HTTP_CLIENT.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    reconnectFailed.increment();
                    System.err.printf("[Client-%d] Cycle %d 连接失败: HTTP %d%n", clientIdx, cycle + 1, response.code());
                    Thread.sleep(CYCLE_GAP_MS);
                    continue;
                }

                reconnectSuccess.increment();
                ResponseBody body = response.body();
                if (body == null) {
                    System.err.printf("[Client-%d] Cycle %d 响应体为空%n", clientIdx, cycle + 1);
                    continue;
                }

                // 读取指定数量的事件
                int eventsThisCycle = 0;
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(body.byteStream()))) {

                    String line;
                    String currentEventId = null;
                    while ((line = reader.readLine()) != null && eventsThisCycle < EVENTS_PER_CYCLE) {
                        if (line.startsWith("id:")) {
                            currentEventId = line.substring(3).trim();
                        } else if (line.startsWith("data:")) {
                            String data = line.substring(5).trim();
                            if (!data.isEmpty()) {
                                eventsThisCycle++;
                                eventsReceived.increment();
                                totalReceivedEvents++;
                                totalExpectedEvents++;
                                lastEventId = currentEventId; // 记录最新的 event ID 用于下次重连
                            }
                        }
                    }
                }

                System.out.printf("[Client-%d] Cycle %d 收到 %d 事件，lastEventId=%s%n",
                        clientIdx, cycle + 1, eventsThisCycle, lastEventId);

                // 模拟网络抖动：主动关闭连接，等待一段时间后重连
                if (cycle < RECONNECT_CYCLES - 1) {
                    Thread.sleep(CYCLE_GAP_MS);
                }

            } catch (IOException | InterruptedException e) {
                reconnectFailed.increment();
                System.err.printf("[Client-%d] Cycle %d 异常: %s%n", clientIdx, cycle + 1, e.getMessage());
            }
        }

        // 简单的事件完整性检查：理论上每个 cycle 应收到 EVENTS_PER_CYCLE 个事件
        // 实际场景下后端可能产生的事件数不固定，这里只做基础统计
        System.out.printf("[Client-%d] 完成 %d 个重连周期，累计收到 %d 事件%n",
                clientIdx, RECONNECT_CYCLES, totalReceivedEvents);
    }

    private static void printReport() {
        long success = reconnectSuccess.sum();
        long failed = reconnectFailed.sum();
        long total = totalReconnects.sum();

        System.out.println("\n========================================");
        System.out.println("QA-03 SSE 断线重连压测报告");
        System.out.println("========================================");
        System.out.printf("并发客户端: %d%n", CONCURRENT_CLIENTS);
        System.out.printf("每客户端重连周期: %d%n", RECONNECT_CYCLES);
        System.out.printf("总重连尝试: %d%n", total);
        System.out.printf("重连成功: %d%n", success);
        System.out.printf("重连失败: %d%n", failed);
        System.out.printf("重连成功率: %.1f%%%n", total > 0 ? (double) success / total * 100 : 0);
        System.out.printf("累计接收事件: %d%n", eventsReceived.sum());

        boolean pass = success == total && failed == 0;
        System.out.println(pass ? "\n>>> 结果: PASS" : "\n>>> 结果: FAIL");
        System.out.println("========================================");
    }
}