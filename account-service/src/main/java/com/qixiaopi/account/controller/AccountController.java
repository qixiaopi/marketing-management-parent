package com.qixiaopi.account.controller;

import com.qixiaopi.account.dto.ResultDTO;
import com.qixiaopi.account.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/account")
public class AccountController {

    @Autowired
    private AccountService accountService;

    @PostMapping("/deduct")
    public ResultDTO<String> deductAccount(@RequestParam String userId, 
                                           @RequestParam Long amount, 
                                           @RequestParam String orderNo) {
        return accountService.deductAccount(userId, amount, orderNo);
    }
}
