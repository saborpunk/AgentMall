package com.example.service;

import cn.hutool.core.util.ObjectUtil;
import com.example.common.enums.ResultCodeEnum;
import com.example.entity.UserAddress;
import com.example.exception.CustomException;
import com.example.mapper.UserAddressMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserAddressService {

    @Resource
    private UserAddressMapper userAddressMapper;

    public void add(UserAddress userAddress) {
        validate(userAddress);
        if (ObjectUtil.isEmpty(userAddress.getIsDefault())) {
            userAddress.setIsDefault(0);
        }
        if (userAddress.getIsDefault() == 1) {
            userAddressMapper.clearDefault(userAddress.getUserId());
        }
        userAddressMapper.insert(userAddress);
    }

    public void updateById(UserAddress userAddress) {
        validate(userAddress);
        if (userAddress.getIsDefault() != null && userAddress.getIsDefault() == 1) {
            userAddressMapper.clearDefault(userAddress.getUserId());
        }
        userAddressMapper.updateById(userAddress);
    }

    public void deleteById(Integer id) {
        userAddressMapper.deleteById(id);
    }

    public List<UserAddress> selectByUserId(Integer userId) {
        if (ObjectUtil.isEmpty(userId)) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        return userAddressMapper.selectByUserId(userId);
    }

    public void setDefault(Integer id, Integer userId) {
        if (ObjectUtil.isEmpty(id) || ObjectUtil.isEmpty(userId)) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        userAddressMapper.clearDefault(userId);
        userAddressMapper.setDefault(id, userId);
    }

    private void validate(UserAddress userAddress) {
        if (ObjectUtil.isEmpty(userAddress.getUserId())
                || ObjectUtil.isEmpty(userAddress.getReceiverName())
                || ObjectUtil.isEmpty(userAddress.getReceiverPhone())
                || ObjectUtil.isEmpty(userAddress.getProvince())
                || ObjectUtil.isEmpty(userAddress.getCity())
                || ObjectUtil.isEmpty(userAddress.getDistrict())
                || ObjectUtil.isEmpty(userAddress.getDetailAddress())) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
    }
}
