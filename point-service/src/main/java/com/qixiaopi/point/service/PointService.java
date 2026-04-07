package com.qixiaopi.point.service;

import com.qixiaopi.point.dto.ResultDTO;

public interface PointService {
    ResultDTO<String> addPoint(Long userId, Long amount, String orderNo, Integer expireDays);
}
