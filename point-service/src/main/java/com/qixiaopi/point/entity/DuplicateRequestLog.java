package com.qixiaopi.point.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_duplicate_request_log")
public class DuplicateRequestLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String userId;
    private String orderNo;
    private String requestType;
    private String rejectReason;
    private LocalDateTime createTime;
}
