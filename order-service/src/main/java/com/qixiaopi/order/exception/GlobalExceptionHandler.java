package com.qixiaopi.order.exception;

import com.qixiaopi.order.dto.ResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ResultDTO<String> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return ResultDTO.failure(e.getMessage());
    }

    /**
     * 处理参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResultDTO<String> handleValidationException(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("参数校验异常: {}", errorMessage);
        return ResultDTO.failure(errorMessage);
    }

    /**
     * 处理系统异常
     */
    @ExceptionHandler(Exception.class)
    public ResultDTO<String> handleSystemException(Exception e) {
        // 检查是否是 Seata 包装的异常，尝试获取原始业务异常
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof BusinessException) {
                log.warn("业务异常(Seata包装): {}", cause.getMessage());
                return ResultDTO.failure(cause.getMessage());
            }
            // 检查是否是 Feign 调用异常
            if (cause.getClass().getName().contains("FeignException")) {
                log.warn("Feign调用异常: {}", cause.getMessage());
                return ResultDTO.failure("服务调用失败，请稍后重试");
            }
            cause = cause.getCause();
        }
        log.error("系统异常: {}", e.getMessage(), e);
        return ResultDTO.failure("系统内部错误，请稍后重试");
    }
}