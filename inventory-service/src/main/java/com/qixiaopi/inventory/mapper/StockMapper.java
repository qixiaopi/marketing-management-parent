package com.qixiaopi.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qixiaopi.inventory.entity.Stock;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface StockMapper extends BaseMapper<Stock> {
    
    /**
     * 查询库存
     */
    @Select("SELECT * FROM t_stock WHERE sku_id = #{skuId} FOR UPDATE ")
    Stock selectById(@Param("skuId") Long skuId);
    
    @Update("UPDATE t_stock SET available_stock = available_stock - #{num}, version = version + 1 WHERE sku_id = #{skuId} AND available_stock >= #{num}")
    int deductStock(@Param("skuId") Long skuId, @Param("num") Integer num);
}
