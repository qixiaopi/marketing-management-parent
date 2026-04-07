package com.qixiaopi.point.controller;

import com.qixiaopi.point.dto.ResultDTO;
import com.qixiaopi.point.service.PointService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
        if (expireDays != null && expireDays > 0) {
            return pointService.addPoint(userId, amount, orderNo, expireDays);
        } else {
            return pointService.addPoint(userId, amount, orderNo,null);
        }
    }
}
