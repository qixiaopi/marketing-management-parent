package com.qixiaopi.account.util;

import org.springframework.util.StringUtils;

public class ValidationUtil {

    /**
     * 校验用户ID
     */
    public static String validateUserId(String userId) {
        if (!StringUtils.hasText(userId)) {
            return "用户ID不能为空";
        }
        return null;
    }

    /**
     * 校验金额
     */
    public static String validateAmount(Long amount) {
        if (amount == null) {
            return "金额不能为空";
        }
        if (amount <= 0) {
            return "金额必须大于0";
        }
        return null;
    }

    /**
     * 校验订单号
     */
    public static String validateOrderNo(String orderNo) {
        if (!StringUtils.hasText(orderNo)) {
            return "订单号不能为空";
        }
        if (orderNo.length() > 64) {
            return "订单号长度不能超过64位";
        }
        return null;
    }
}
