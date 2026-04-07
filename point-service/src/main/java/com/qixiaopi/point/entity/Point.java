package com.qixiaopi.point.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_point")
public class Point {
    private Long id;
    private Long userId;
    private Long point;
    private Integer version;
    private LocalDateTime expireTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
