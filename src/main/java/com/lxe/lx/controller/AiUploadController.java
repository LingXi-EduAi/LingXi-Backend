package com.lxe.lx.controller;

import com.lxe.lx.annotation.Login;
import com.lxe.lx.domain.dto.AiApiResponse;
import com.lxe.lx.pojo.TokenEntity;
import com.lxe.lx.service.AiTaskApiException;
import com.lxe.lx.util.FileUpload;
import com.lxe.lx.util.Tools;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.lxe.lx.config.AuthorizationInterceptor.ORG_ID_KEY;

/**
 * AI 域附件上传端点。
 *
 * <p>{@code POST /api/ai/upload}：接收 multipart 文件（{@code @Login} 鉴权），
 * 校验大小与扩展名白名单后存储到项目统一的 configurable path，
 * 返回 {@code {fileId, url}}，供前端 AiChatDialog 上传链路使用
 * （fileId/url 会写入 AI 消息的 attachments）。
 *
 * <p>存储与 URL 拼接复用现有 {@link UploadController} 的约定：
 * 文件写入 {@code FILE_PATH_UPLOAD}，URL 由 {@code SERVER_PATH} + 相对路径拼出。
 */
@RestController
@RequestMapping("/api/ai")
public class AiUploadController {

    private static final Logger logger = LogManager.getLogger(AiUploadController.class);

    /** 单文件大小上限（MB），与 spring.servlet.multipart.max-file-size 一致。 */
    static final long MAX_FILE_SIZE_MB = 50L;

    /** 允许上传的扩展名白名单（小写，不含点）。 */
    static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList(
            // 图片
            "jpg", "jpeg", "png", "gif", "webp", "bmp",
            // 文档
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "md"
    ));

    @Login
    @PostMapping("/upload")
    public AiApiResponse<Map<String, String>> upload(
            HttpServletRequest request,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        String requestId = requestId();
        String userId = currentUserId(request);

        if (file == null || file.isEmpty()) {
            throw new AiTaskApiException(400, "上传文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE_MB * 1024 * 1024) {
            throw new AiTaskApiException(400, "文件大小不能超过" + MAX_FILE_SIZE_MB + "M");
        }
        String extension = extensionOf(file.getOriginalFilename());
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension)) {
            throw new AiTaskApiException(400, "不支持的文件类型");
        }

        String storedName;
        try {
            storedName = FileUpload.fileUp(file, Tools.getConfigValue("FILE_PATH_UPLOAD"), null);
        } catch (IOException e) {
            logger.error("AI upload failed, userId={}, requestId={}", userId, requestId, e);
            throw new AiTaskApiException(500, "文件上传失败");
        }

        String fileId = storedName;
        logger.info("AI upload success, userId={}, fileId={}, requestId={}", userId, fileId, requestId);

        return AiApiResponse.success(requestId, buildResult(storedName));
    }

    /** 由存储文件名拼出 {fileId, url}。fileId 即存储文件名，url 走 SERVER_PATH 约定。 */
    static Map<String, String> buildResult(String storedName) {
        Map<String, String> data = new HashMap<>();
        data.put("fileId", storedName);
        try {
            data.put("url", Tools.getConfigValue("SERVER_PATH") + "uploadFilesTest/file/" + storedName);
        } catch (IOException e) {
            throw new AiTaskApiException(500, "文件上传失败");
        }
        return data;
    }

    private String currentUserId(HttpServletRequest request) {
        TokenEntity tokenEntity = (TokenEntity) request.getAttribute(ORG_ID_KEY);
        if (tokenEntity == null || StringUtils.isBlank(tokenEntity.getId())) {
            throw new AiTaskApiException(401, "无法获取当前登录用户");
        }
        return tokenEntity.getId();
    }

    private String requestId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /** 提取小写扩展名（不含点）；无扩展名返回 null。 */
    static String extensionOf(String originalFilename) {
        if (StringUtils.isBlank(originalFilename)) {
            return null;
        }
        int dot = originalFilename.lastIndexOf('.');
        if (dot < 0 || dot == originalFilename.length() - 1) {
            return null;
        }
        return originalFilename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
