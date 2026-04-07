package com.qixiaopi.inventory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_inventory_log")
public class InventoryLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long skuId;
    private String orderNo;
    private Integer changeQuantity;
    private Integer beforeStock;
    private Integer afterStock;
    private LocalDateTime createTime;
}
