package com.lxe.lx.config;

import com.lxe.lx.util.ResultConstant;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 统一处理系统中的各种异常，提供友好的错误信息
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LogManager.getLogger(GlobalExceptionHandler.class);
    
    /**
     * 处理参数验证异常
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResultConstant handleValidationException(Exception e, HttpServletRequest request) {
        logger.warn("参数验证失败: {}, 请求路径: {}", e.getMessage(), request.getRequestURI());
        
        String errorMessage = "参数验证失败";
        if (e instanceof MethodArgumentNotValidException) {
            MethodArgumentNotValidException ex = (MethodArgumentNotValidException) e;
            errorMessage = ex.getBindingResult().getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .collect(Collectors.joining(", "));
        } else if (e instanceof BindException) {
            BindException ex = (BindException) e;
            errorMessage = ex.getBindingResult().getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .collect(Collectors.joining(", "));
        }
        
        return ResultConstant.illegalParams(errorMessage);
    }
    
    /**
     * 处理数据库相关异常
     */
    @ExceptionHandler({SQLException.class, DataIntegrityViolationException.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResultConstant handleDatabaseException(Exception e, HttpServletRequest request) {
        logger.error("数据库操作异常: {}, 请求路径: {}", e.getMessage(), request.getRequestURI(), e);
        
        if (e instanceof DataIntegrityViolationException) {
            return ResultConstant.error("数据完整性约束失败，请检查数据是否重复或关联关系是否正确");
        }
        
        return ResultConstant.error("数据库操作失败，请联系管理员");
    }
    
    /**
     * 处理文件上传异常
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResultConstant handleFileUploadException(MaxUploadSizeExceededException e, HttpServletRequest request) {
        logger.warn("文件上传大小超限: {}, 请求路径: {}", e.getMessage(), request.getRequestURI());
        return ResultConstant.error("上传文件大小超过限制，请选择较小的文件");
    }
    
    /**
     * 处理空指针异常
     */
    @ExceptionHandler(NullPointerException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResultConstant handleNullPointerException(NullPointerException e, HttpServletRequest request) {
        logger.error("空指针异常: {}, 请求路径: {}", e.getMessage(), request.getRequestURI(), e);
        return ResultConstant.error("系统内部错误，请联系管理员");
    }
    
    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResultConstant handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        logger.warn("非法参数异常: {}, 请求路径: {}", e.getMessage(), request.getRequestURI());
        return ResultConstant.illegalParams(e.getMessage());
    }
    
    /**
     * 处理通用运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResultConstant handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        logger.error("运行时异常: {}, 请求路径: {}", e.getMessage(), request.getRequestURI(), e);
        return ResultConstant.error("系统繁忙，请稍后重试");
    }
    
    /**
     * 处理其他未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResultConstant handleGeneralException(Exception e, HttpServletRequest request) {
        logger.error("未知异常: {}, 请求路径: {}", e.getMessage(), request.getRequestURI(), e);
        return ResultConstant.error("系统异常，请联系管理员");
    }
}
