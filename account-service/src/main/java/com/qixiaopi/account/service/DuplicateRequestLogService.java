package com.qixiaopi.account.service;

public interface DuplicateRequestLogService {
    void logDuplicateRequest(String userId, String orderNo, String requestType, String rejectReason);
}
