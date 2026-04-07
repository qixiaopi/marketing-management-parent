package com.qixiaopi.point.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qixiaopi.point.entity.PointLog;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface PointLogMapper extends BaseMapper<PointLog> {

    /**
     * 根据订单号查询流水记录（只查询id，用于幂等性检查）
     */
    @Select("SELECT id FROM t_point_log WHERE order_no = #{orderNo} LIMIT 1")
    Long selectIdByOrderNo(@Param("orderNo") String orderNo);
}
