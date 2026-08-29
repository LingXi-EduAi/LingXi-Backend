package com.lxe.lx.controller;

import com.lxe.lx.pojo.TokenEntity;
import com.lxe.lx.service.AiTaskApiException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

import java.util.Map;

import static com.lxe.lx.config.AuthorizationInterceptor.ORG_ID_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * QA-01 场景：AI 域附件上传端点 {@code POST /api/ai/upload}。
 * 断言成功返回 fileId/url、超限拒绝、非法扩展名拒绝、空文件/未鉴权拒绝。
 */
class AiUploadControllerTests {

    private final AiUploadController controller = new AiUploadController();

    @Test
    void buildResultReturnsFileIdAndUrl() {
        Map<String, String> result = AiUploadController.buildResult("abc123.png");

        assertEquals("abc123.png", result.get("fileId"));
        assertEquals("http://localhost:5678uploadFilesTest/file/abc123.png", result.get("url"));
    }

    @Test
    void uploadRejectsOversizeFile() {
        HttpServletRequest request = request("user-1");
        MultipartFile file = file("big.pdf", 51L * 1024 * 1024);

        AiTaskApiException exception = assertThrows(AiTaskApiException.class,
                () -> controller.upload(request, file));

        assertEquals(400, exception.getHttpStatus());
    }

    @Test
    void uploadRejectsUnsupportedExtension() {
        HttpServletRequest request = request("user-1");
        MultipartFile file = file("virus.exe", 1024);

        AiTaskApiException exception = assertThrows(AiTaskApiException.class,
                () -> controller.upload(request, file));

        assertEquals(400, exception.getHttpStatus());
    }

    @Test
    void uploadRejectsEmptyFile() {
        HttpServletRequest request = request("user-1");
        MultipartFile file = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        AiTaskApiException exception = assertThrows(AiTaskApiException.class,
                () -> controller.upload(request, file));

        assertEquals(400, exception.getHttpStatus());
    }

    @Test
    void uploadRejectsMissingUser() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        MultipartFile file = file("photo.png", 1024);

        AiTaskApiException exception = assertThrows(AiTaskApiException.class,
                () -> controller.upload(request, file));

        assertEquals(401, exception.getHttpStatus());
    }

    @Test
    void extensionOfReturnsLowercaseExtension() {
        assertEquals("png", AiUploadController.extensionOf("photo.PNG"));
        assertEquals("pdf", AiUploadController.extensionOf("report.pdf"));
        assertEquals("docx", AiUploadController.extensionOf("a.b.docx"));
    }

    @Test
    void extensionOfReturnsNullForNoExtension() {
        assertNull(AiUploadController.extensionOf("noextension"));
        assertNull(AiUploadController.extensionOf("trailing."));
        assertNull(AiUploadController.extensionOf(""));
        assertNull(AiUploadController.extensionOf(null));
    }

    private HttpServletRequest request(String userId) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        TokenEntity token = new TokenEntity();
        token.setId(userId);
        when(request.getAttribute(ORG_ID_KEY)).thenReturn(token);
        return request;
    }

    private MultipartFile file(String name, long size) {
        return new MockMultipartFile("file", name, "application/octet-stream", new byte[(int) size]);
    }
}
