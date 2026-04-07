package com.qixiaopi.account.service;

import com.qixiaopi.account.dto.ResultDTO;

public interface AccountService {
    ResultDTO<String> deductAccount(String userId, Long amount, String orderNo);
}
