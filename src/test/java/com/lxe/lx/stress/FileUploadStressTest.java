package com.lxe.lx.stress;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;

/**
 * QA-03 压力测试：大文件上传并发压测
 *
 * 运行前提：
 *   - 后端已启动（默认 http://localhost:5678）
 *   - 已存在有效用户 token
 *   - 测试文件已准备（可通过 -Dstress.upload.file 指定）
 *
 * 运行方式：
 *   mvn test -Dtest=FileUploadStressTest -DfailIfNoTests=false
 *   -Dstress.token=xxx -Dstress.upload.file=path/to/large.pdf -Dstress.upload.count=20
 *
 * 指标：
 *   - 上传成功率
 *   - 平均上传耗时
 *   - 吞吐 MB/s
 *   - 错误分类
 */
public class FileUploadStressTest {

    private static final String BASE_URL = System.getProperty("stress.baseUrl", "http://localhost:5678");
    private static final String AUTH_TOKEN = System.getProperty("stress.token", "");
    private static final String UPLOAD_FILE_PATH = System.getProperty("stress.upload.file", "");
    private static final int CONCURRENT_UPLOADS = Integer.parseInt(System.getProperty("stress.upload.count", "20"));

    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(30))
            .readTimeout(Duration.ofSeconds(120))
            .writeTimeout(Duration.ofSeconds(120))
            .build();

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final LongAdder uploadsSucceeded = new LongAdder();
    private static final LongAdder uploadsFailed = new LongAdder();
    private static final LongAdder totalBytesUploaded = new LongAdder();
    private static final List<Long> uploadDurationsMs = new CopyOnWriteArrayList<>();

    public static void main(String[] args) throws Exception {
        if (AUTH_TOKEN.isEmpty() || UPLOAD_FILE_PATH.isEmpty()) {
            System.err.println("ERROR: 必须提供 -Dstress.token=<token> -Dstress.upload.file=<path>");
            System.exit(1);
        }

        File testFile = new File(UPLOAD_FILE_PATH);
        if (!testFile.exists()) {
            System.err.println("ERROR: 测试文件不存在: " + UPLOAD_FILE_PATH);
            System.exit(1);
        }

        long fileSize = testFile.length();
        System.out.println("========================================");
        System.out.println("QA-03 大文件上传并发压测启动");
        System.out.println("BASE_URL: " + BASE_URL);
        System.out.println("CONCURRENT_UPLOADS: " + CONCURRENT_UPLOADS);
        System.out.println("测试文件: " + testFile.getName() + " (" + formatSize(fileSize) + ")");
        System.out.println("========================================");

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_UPLOADS);
        CountDownLatch startLatch = new CountDownLatch(CONCURRENT_UPLOADS);
        CountDownLatch doneLatch = new CountDownLatch(CONCURRENT_UPLOADS);

        Instant testStart = Instant.now();

        for (int i = 0; i < CONCURRENT_UPLOADS; i++) {
            final int idx = i;
            executor.submit(() -> {
                startLatch.countDown();
                try { startLatch.await(); } catch (InterruptedException ignored) {}
                runSingleUpload(idx, testFile);
                doneLatch.countDown();
            });
        }

        doneLatch.await();
        executor.shutdown();

        Instant testEnd = Instant.now();
        printReport(Duration.between(testStart, testEnd), fileSize);
    }

    private static void runSingleUpload(int idx, File file) {
        Instant start = Instant.now();

        RequestBody fileBody = RequestBody.create(MediaType.parse("application/octet-stream"), file);
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getName(), fileBody)
                .build();

        Request request = new Request.Builder()
                .url(BASE_URL + "/api/ai/upload")
                .addHeader("Authorization", "Bearer " + AUTH_TOKEN)
                .post(requestBody)
                .build();

        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            long durationMs = Duration.between(start, Instant.now()).toMillis();
            uploadDurationsMs.add(durationMs);

            if (response.isSuccessful()) {
                uploadsSucceeded.increment();
                totalBytesUploaded.add(file.length());
                System.out.printf("[Upload-%d] 成功，耗时 %d ms%n", idx, durationMs);
            } else {
                uploadsFailed.increment();
                String err = response.body() != null ? response.body().string() : "no body";
                System.err.printf("[Upload-%d] 失败 HTTP %d: %s%n", idx, response.code(), err);
            }
        } catch (IOException e) {
            uploadsFailed.increment();
            long durationMs = Duration.between(start, Instant.now()).toMillis();
            uploadDurationsMs.add(durationMs);
            System.err.printf("[Upload-%d] 异常: %s%n", idx, e.getMessage());
        }
    }

    private static void printReport(Duration totalDuration, long fileSize) {
        long succeeded = uploadsSucceeded.sum();
        long failed = uploadsFailed.sum();
        long total = succeeded + failed;

        System.out.println("\n========================================");
        System.out.println("QA-03 文件上传压测报告");
        System.out.println("========================================");
        System.out.printf("测试总时长: %d 秒%n", totalDuration.getSeconds());
        System.out.printf("并发上传数: %d%n", CONCURRENT_UPLOADS);
        System.out.printf("单文件大小: %s%n", formatSize(fileSize));
        System.out.printf("上传成功: %d%n", succeeded);
        System.out.printf("上传失败: %d%n", failed);
        System.out.printf("成功率: %.1f%%%n", total > 0 ? (double) succeeded / total * 100 : 0);

        if (succeeded > 0) {
            long totalBytes = totalBytesUploaded.sum();
            double throughputMBps = (totalBytes / 1024.0 / 1024.0) / totalDuration.getSeconds();
            System.out.printf("总上传量: %s%n", formatSize(totalBytes));
            System.out.printf("吞吐率: %.2f MB/s%n", throughputMBps);

            List<Long> durations = new ArrayList<>(uploadDurationsMs);
            durations.sort(Long::compareTo);
            long p50 = durations.get(durations.size() / 2);
            long p95 = durations.get((int) (durations.size() * 0.95));
            long avg = durations.stream().mapToLong(Long::longValue).sum() / durations.size();
            System.out.printf("上传耗时 平均: %d ms, P50: %d ms, P95: %d ms%n", avg, p50, p95);
        }

        boolean pass = succeeded == CONCURRENT_UPLOADS && failed == 0;
        System.out.println(pass ? "\n>>> 结果: PASS" : "\n>>> 结果: FAIL");
        System.out.println("========================================");
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / 1024.0 / 1024.0);
        return String.format("%.1f GB", bytes / 1024.0 / 1024.0 / 1024.0);
    }
}