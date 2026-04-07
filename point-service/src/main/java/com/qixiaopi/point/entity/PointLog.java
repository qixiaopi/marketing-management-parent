package com.qixiaopi.point.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_point_log")
public class PointLog {
    private Long id;
    private Long userId;
    private String orderNo;
    private Long changePoint;
    private Long beforePoint;
    private Long afterPoint;
    private Integer type;
    private String remark;
    private LocalDateTime createTime;
}
