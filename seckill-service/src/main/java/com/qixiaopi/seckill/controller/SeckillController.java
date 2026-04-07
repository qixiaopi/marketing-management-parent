package com.qixiaopi.seckill.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.qixiaopi.seckill.service.SeckillService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/seckill")
@RequiredArgsConstructor
public class SeckillController {
	
    private final  SeckillService seckillService;
    /**
     * 秒杀接口
     */
    @PostMapping("/do/{productId}")
    public String doSeckill(@PathVariable Long productId, @RequestParam Long userId) {
        // 参数验证
        if (productId == null || productId <= 0) {
            return "商品ID无效";
        }
        if (userId == null || userId <= 0) {
            return "用户ID无效";
        }
        String result = seckillService.doSeckill(userId, productId, 1);
        return result;
    }
}
