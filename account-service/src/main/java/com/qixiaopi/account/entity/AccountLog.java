package com.qixiaopi.account.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_account_log")
public class AccountLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String userId;
    private String orderNo;
    private Long changeAmount;
    private Long beforeBalance;
    private Long afterBalance;
    private Integer type;
    private String remark;
    private LocalDateTime createTime;
}
