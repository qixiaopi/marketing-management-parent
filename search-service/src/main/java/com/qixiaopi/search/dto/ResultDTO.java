package com.qixiaopi.search.dto;

import lombok.Data;

@Data
public class ResultDTO<T> {
    private boolean success;
    private T data;
    private String message;
    
    public ResultDTO() {
    }
    
    public ResultDTO(boolean success, T data, String message) {
        this.success = success;
        this.data = data;
        this.message = message;
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
    
    public static <T> ResultDTO<T> failure(T data, String message) {
        return new ResultDTO<>(false, data, message);
    }
}
