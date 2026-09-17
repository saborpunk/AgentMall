package com.example.controller;

import com.example.common.Result;
import com.example.entity.User;
import com.example.entity.WalletRechargeRequest;
import com.example.entity.WalletRecord;
import com.example.service.WalletRecordService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/wallet")
public class WalletRecordController {

    @Resource
    private WalletRecordService walletRecordService;

    @PostMapping("/recharge")
    public Result recharge(@RequestBody WalletRechargeRequest request) {
        User user = walletRecordService.recharge(request);
        return Result.success(user);
    }

    @GetMapping("/records/{userId}")
    public Result records(@PathVariable Integer userId) {
        List<WalletRecord> list = walletRecordService.selectByUserId(userId);
        return Result.success(list);
    }
}
