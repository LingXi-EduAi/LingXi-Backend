package com.lxe.lx.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lxe.lx.annotation.Login;
import com.lxe.lx.annotation.TeacherOnly;
import com.lxe.lx.domain.dto.AiApiResponse;
import com.lxe.lx.domain.dto.AiModelCallLogPage;
import com.lxe.lx.domain.qo.AiModelCallLogQuery;
import com.lxe.lx.pojo.AiModelCallLog;
import com.lxe.lx.pojo.TokenEntity;
import com.lxe.lx.service.AiModelCallLogService;
import com.lxe.lx.service.AiTaskApiException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.lxe.lx.config.AuthorizationInterceptor.ORG_ID_KEY;

@RestController
@RequestMapping("/api/ai/model-calls")
@TeacherOnly
public class AiModelCallLogController {
    private final AiModelCallLogService service;
    private final ObjectMapper objectMapper;

    public AiModelCallLogController(AiModelCallLogService service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    @Login
    @GetMapping
    public AiApiResponse<AiModelCallLogPage> list(
            HttpServletRequest request,
            @RequestParam(required = false) String taskId,
            @RequestParam(required = false) String nodeName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime startAt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime endAt,
            @RequestParam(defaultValue = "1") int currentPage,
            @RequestParam(defaultValue = "20") int pageSize) {
        AiModelCallLogQuery query = query(request, taskId, nodeName, startAt, endAt, currentPage, pageSize);
        List<AiModelCallLog> page = service.findByQuery(query);
        List<AiModelCallLog> all = service.findAllByQuery(query);
        AiModelCallLogPage result = aggregate(page, all, currentPage, pageSize, service.countByQuery(query));
        return AiApiResponse.success(requestId(), result);
    }

    @Login
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            HttpServletRequest request,
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(required = false) String taskId,
            @RequestParam(required = false) String nodeName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime startAt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime endAt) {
        AiModelCallLogQuery query = query(request, taskId, nodeName, startAt, endAt, 1, 1);
        List<AiModelCallLog> logs = service.findAllByQuery(query);
        byte[] body;
        String contentType;
        String extension;
        try {
            if ("json".equalsIgnoreCase(format)) {
                body = objectMapper.writeValueAsBytes(logs);
                contentType = MediaType.APPLICATION_JSON_VALUE;
                extension = "json";
            } else if ("csv".equalsIgnoreCase(format)) {
                body = csv(logs);
                contentType = "text/csv; charset=UTF-8";
                extension = "csv";
            } else {
                throw new AiTaskApiException(400, "format 只能是 csv 或 json");
            }
        } catch (AiTaskApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AiTaskApiException(500, "模型调用日志导出失败");
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ai-model-calls." + extension)
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .body(body);
    }

    private AiModelCallLogQuery query(
            HttpServletRequest request, String taskId, String nodeName,
            LocalDateTime startAt, LocalDateTime endAt, int page, int size) {
        if (page < 1 || size < 1 || size > 1000) {
            throw new AiTaskApiException(400, "分页参数无效");
        }
        AiModelCallLogQuery query = new AiModelCallLogQuery();
        query.setUserId(currentUserId(request));
        query.setTaskId(taskId);
        query.setNodeName(nodeName);
        query.setStartAt(startAt);
        query.setEndAt(endAt);
        query.setOffset((page - 1) * size);
        query.setLimit(size);
        return query;
    }

    private AiModelCallLogPage aggregate(
            List<AiModelCallLog> page, List<AiModelCallLog> all,
            int currentPage, int pageSize, int total) {
        AiModelCallLogPage result = new AiModelCallLogPage();
        result.setList(page);
        result.setTotal(total);
        result.setCurrentPage(currentPage);
        result.setPageSize(pageSize);
        long latencyTotal = 0;
        for (AiModelCallLog log : all) {
            if (log.getTotalTokens() != null) {
                result.setTotalTokens(result.getTotalTokens() + log.getTotalTokens());
            }
            if (log.getCost() != null) {
                result.setTotalCost(result.getTotalCost().add(log.getCost()));
            }
            if (log.getLatencyMs() != null) {
                latencyTotal += log.getLatencyMs();
            }
            if (StringUtils.isNotBlank(log.getErrorCode())) {
                result.setFailedCount(result.getFailedCount() + 1);
            }
        }
        result.setAverageLatencyMs(all.isEmpty() ? 0 : latencyTotal / all.size());
        return result;
    }

    private byte[] csv(List<AiModelCallLog> logs) {
        StringBuilder csv = new StringBuilder("\uFEFFtime,user,task,app,model,tokens,latency,cost,error\n");
        for (AiModelCallLog log : logs) {
            csv.append(value(log.getCreatedAt())).append(',')
                    .append(value(log.getUserId())).append(',')
                    .append(value(log.getTaskId())).append(',')
                    .append(value(log.getNodeName())).append(',')
                    .append(value(log.getModel())).append(',')
                    .append(value(log.getTotalTokens())).append(',')
                    .append(value(log.getLatencyMs())).append(',')
                    .append(value(log.getCost())).append(',')
                    .append(value(log.getErrorCode())).append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String value(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value).replace("\"", "\"\"");
        return text.contains(",") || text.contains("\n") ? "\"" + text + "\"" : text;
    }

    private String currentUserId(HttpServletRequest request) {
        TokenEntity token = (TokenEntity) request.getAttribute(ORG_ID_KEY);
        if (token == null || StringUtils.isBlank(token.getId())) {
            throw new AiTaskApiException(401, "无法获取当前登录用户");
        }
        return token.getId();
    }

    private String requestId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
