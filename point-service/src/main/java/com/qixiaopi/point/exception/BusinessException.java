package com.qixiaopi.point.exception;

public class BusinessException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private String errorCode;
    
    public BusinessException(String message) {
        super(message);
    }
    
    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public BusinessException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}