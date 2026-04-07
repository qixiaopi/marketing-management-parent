package com.qixiaopi.account.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 金额单位转换工具类
 * 数据库中存储单位为"分"，避免浮点数精度问题
 */
public class MoneyUtil {

    /**
     * 将元转换为分
     * @param yuan 金额（元）
     * @return 金额（分）
     */
    public static Long yuanToFen(BigDecimal yuan) {
        if (yuan == null) {
            return 0L;
        }
        return yuan.multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).longValue();
    }

    /**
     * 将元转换为分
     * @param yuan 金额（元）
     * @return 金额（分）
     */
    public static Long yuanToFen(String yuan) {
        if (yuan == null || yuan.isEmpty()) {
            return 0L;
        }
        return yuanToFen(new BigDecimal(yuan));
    }

    /**
     * 将元转换为分
     * @param yuan 金额（元）
     * @return 金额（分）
     */
    public static Long yuanToFen(double yuan) {
        return yuanToFen(BigDecimal.valueOf(yuan));
    }

    /**
     * 将分转换为元
     * @param fen 金额（分）
     * @return 金额（元）
     */
    public static BigDecimal fenToYuan(Long fen) {
        if (fen == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(fen).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    /**
     * 将分转换为元（字符串格式）
     * @param fen 金额（分）
     * @return 金额（元）字符串
     */
    public static String fenToYuanStr(Long fen) {
        return fenToYuan(fen).toString();
    }

    /**
     * 格式化金额显示
     * @param fen 金额（分）
     * @return 格式化后的金额字符串，如：¥1,234.56
     */
    public static String formatMoney(Long fen) {
        if (fen == null) {
            return "¥0.00";
        }
        return "¥" + fenToYuan(fen).toString();
    }
}
