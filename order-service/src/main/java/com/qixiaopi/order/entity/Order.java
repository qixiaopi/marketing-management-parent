package com.qixiaopi.order.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("t_order")
public class Order implements Serializable {
	private static final long serialVersionUID = 7272865251216862943L;
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String orderNo;
    private Long userId;
    private Long skuId;
    private Integer buyNum;
    private Long orderAmount;
    private Integer orderStatus;
    private String receiveName;
    private String receivePhone;
    private String receiveAddress;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
