package com.lxe.lx.exception;

import com.lxe.lx.controller.AiConversationController;
import com.lxe.lx.controller.AiModelCallLogController;
import com.lxe.lx.controller.AiTaskController;
import com.lxe.lx.controller.AiUploadController;
import com.lxe.lx.domain.dto.AiApiResponse;
import com.lxe.lx.service.AiTaskApiException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.UUID;

@RestControllerAdvice(assignableTypes = {
        AiTaskController.class, AiConversationController.class, AiModelCallLogController.class,
        AiUploadController.class})
public class AiTaskExceptionHandler {

    @ExceptionHandler(AiTaskApiException.class)
    public ResponseEntity<AiApiResponse<Void>> handle(AiTaskApiException exception) {
        String requestId = UUID.randomUUID().toString().replace("-", "");
        return ResponseEntity.status(exception.getHttpStatus())
                .body(AiApiResponse.error(exception.getHttpStatus(), exception.getMessage(), requestId));
    }
}
