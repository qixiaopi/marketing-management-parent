package com.qixiaopi.point.exception;

import com.qixiaopi.point.dto.ResultDTO;
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
        return ResultDTO.failure(e.getErrorCode(), e.getMessage());
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
        log.error("系统异常: {}", e.getMessage(), e);
        return ResultDTO.failure("系统内部错误，请稍后重试");
    }
}