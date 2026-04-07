package com.qixiaopi.point.service;

public interface DuplicateRequestLogService {
    void logDuplicateRequest(String userId, String orderNo, String requestType, String rejectReason);
}
