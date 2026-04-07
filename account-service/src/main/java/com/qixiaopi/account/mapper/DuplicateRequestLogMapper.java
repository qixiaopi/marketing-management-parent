package com.qixiaopi.account.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qixiaopi.account.entity.DuplicateRequestLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

public interface DuplicateRequestLogMapper extends BaseMapper<DuplicateRequestLog> {

    @Insert("INSERT INTO t_duplicate_request_log(user_id, order_no, request_type, reject_reason, request_time) " +
            "VALUES(#{userId}, #{orderNo}, #{requestType}, #{rejectReason}, #{requestTime})")
    int insertLog(@Param("userId") String userId,
                  @Param("orderNo") String orderNo,
                  @Param("requestType") String requestType,
                  @Param("rejectReason") String rejectReason,
                  @Param("requestTime") java.time.LocalDateTime requestTime);
}
