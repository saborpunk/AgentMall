package com.example.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.example.common.enums.ResultCodeEnum;
import com.example.entity.User;
import com.example.entity.WalletRechargeRequest;
import com.example.entity.WalletRecord;
import com.example.exception.CustomException;
import com.example.mapper.UserMapper;
import com.example.mapper.WalletRecordMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class WalletRecordService {

    @Resource
    private WalletRecordMapper walletRecordMapper;
    @Resource
    private UserMapper userMapper;

    public User recharge(WalletRechargeRequest request) {
        if (ObjectUtil.isEmpty(request.getUserId()) || request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        User user = userMapper.selectById(request.getUserId());
        if (ObjectUtil.isNull(user)) {
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        userMapper.increaseBalance(request.getUserId(), request.getAmount());
        User updatedUser = userMapper.selectById(request.getUserId());

        WalletRecord walletRecord = new WalletRecord();
        walletRecord.setUserId(request.getUserId());
        walletRecord.setType("RECHARGE");
        walletRecord.setAmount(request.getAmount());
        walletRecord.setBalanceAfter(updatedUser.getBalance());
        walletRecord.setBusinessNo("RC" + DateUtil.format(DateUtil.date(), "yyyyMMddHHmmssSSS"));
        walletRecord.setRemark(ObjectUtil.isEmpty(request.getRemark()) ? "用户钱包充值" : request.getRemark());
        walletRecordMapper.insert(walletRecord);
        return updatedUser;
    }

    public List<WalletRecord> selectByUserId(Integer userId) {
        if (ObjectUtil.isEmpty(userId)) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        return walletRecordMapper.selectByUserId(userId);
    }
}
