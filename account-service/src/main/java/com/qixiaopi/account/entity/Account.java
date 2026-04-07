package com.qixiaopi.account.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_account")
public class Account {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String userId;
    private Long balance;
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
