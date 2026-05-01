package com.qixiaopi.point.controller;

import com.qixiaopi.point.dto.ResultDTO;
import com.qixiaopi.point.service.PointService;
import io.seata.core.context.RootContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@RestController
@RequestMapping("/point")
public class PointController {

    @Autowired
    private PointService pointService;

    @PostMapping("/add")
    public ResultDTO<String> addPoint(@RequestParam Long userId, 
                                       @RequestParam Long amount, 
                                       @RequestParam String orderNo,
                                       @RequestParam(required = false) Integer expireDays) {
        // 从请求头中获取XID
        String xid = null;
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            xid = attributes.getRequest().getHeader(RootContext.KEY_XID);
            log.info("从请求头获取到XID: {}", xid);
            if (xid != null) {
                RootContext.bind(xid);
                log.info("绑定XID到RootContext: {}", xid);
            }
        }
        
        // 打印当前XID，用于调试
        String currentXid = RootContext.getXID();
        log.info("积分服务当前XID: {}", currentXid);
        
        if (expireDays != null && expireDays > 0) {
            return pointService.addPoint(userId, amount, orderNo, expireDays);
        } else {
            return pointService.addPoint(userId, amount, orderNo,null);
        }
    }
}
