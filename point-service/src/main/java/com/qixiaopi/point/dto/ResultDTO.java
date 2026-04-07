package com.qixiaopi.point.dto;

public class ResultDTO<T> {
    private boolean success;
    private T data;
    private String message;
    private String errorCode;

    public ResultDTO() {
    }

    public ResultDTO(boolean success, T data, String message) {
        this.success = success;
        this.data = data;
        this.message = message;
    }

    public ResultDTO(boolean success, T data, String message, String errorCode) {
        this.success = success;
        this.data = data;
        this.message = message;
        this.errorCode = errorCode;
    }

    public boolean isSuccess() {
        return success;
    }

    public T getData() {
        return data;
    }

    public String getMessage() {
        return message;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public static <T> ResultDTO<T> success(T data) {
        return new ResultDTO<>(true, data, "成功");
    }

    public static <T> ResultDTO<T> success(T data, String message) {
        return new ResultDTO<>(true, data, message);
    }

    public static <T> ResultDTO<T> failure(String message) {
        return new ResultDTO<>(false, null, message);
    }

    public static <T> ResultDTO<T> failure(String errorCode, String message) {
        return new ResultDTO<>(false, null, message, errorCode);
    }
}
