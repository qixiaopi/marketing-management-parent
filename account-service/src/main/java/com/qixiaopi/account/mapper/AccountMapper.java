package com.qixiaopi.account.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qixiaopi.account.entity.Account;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface AccountMapper extends BaseMapper<Account> {

    /**
     * 使用行锁查询账户（FOR UPDATE）
     * 确保并发环境下读取的账户数据准确
     */
    @Select("SELECT * FROM t_account WHERE user_id = #{userId} FOR UPDATE")
    Account selectByUserIdForUpdate(@Param("userId") String userId);

    /**
     * 扣减账户余额
     */
    @Update("UPDATE t_account SET balance = balance - #{amount}, version = version + 1 WHERE user_id = #{userId} AND balance >= #{amount}")
    int deduct(@Param("userId") String userId, @Param("amount") Long amount);
}
