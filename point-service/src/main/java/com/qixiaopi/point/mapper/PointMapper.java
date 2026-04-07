package com.qixiaopi.point.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qixiaopi.point.entity.Point;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

public interface PointMapper extends BaseMapper<Point> {

    /**
     * 查询积分（普通查询，不加锁）
     * 用于乐观锁实现
     */
    @Select("SELECT * FROM t_point WHERE user_id = #{userId}")
    Point selectByUserId(@Param("userId") Long userId);
    /**
     * 增加积分并更新过期时间（乐观锁）
     */
    @Update("UPDATE t_point SET point = point + #{amount}, expire_time = #{expireTime}, version = version + 1 WHERE user_id = #{userId} AND version = #{version}")
    int addPointWithExpireAndVersion(@Param("userId") Long userId, @Param("amount") Long amount, @Param("expireTime") LocalDateTime expireTime, @Param("version") Integer version);
}
