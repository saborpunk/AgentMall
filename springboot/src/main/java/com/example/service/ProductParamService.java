package com.example.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.example.common.enums.ResultCodeEnum;
import com.example.entity.ProductParam;
import com.example.exception.CustomException;
import com.example.mapper.ProductParamMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductParamService {

    @Resource
    private ProductParamMapper productParamMapper;

    public void add(ProductParam productParam) {
        validate(productParam);
        if (productParam.getSort() == null) {
            productParam.setSort(1);
        }
        if (productParam.getIsCore() == null) {
            productParam.setIsCore(0);
        }
        String now = DateUtil.now();
        productParam.setCreateTime(now);
        productParam.setUpdateTime(now);
        productParamMapper.insert(productParam);
    }

    public void updateById(ProductParam productParam) {
        if (ObjectUtil.isEmpty(productParam.getId())) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        validate(productParam);
        productParam.setUpdateTime(DateUtil.now());
        productParamMapper.updateById(productParam);
    }

    public void deleteById(Integer id) {
        productParamMapper.deleteById(id);
    }

    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            productParamMapper.deleteById(id);
        }
    }

    public List<ProductParam> selectAll(ProductParam productParam) {
        return productParamMapper.selectAll(productParam);
    }

    private void validate(ProductParam productParam) {
        if (ObjectUtil.isEmpty(productParam.getProductId())
                || ObjectUtil.isEmpty(productParam.getParamGroup())
                || ObjectUtil.isEmpty(productParam.getParamName())
                || ObjectUtil.isEmpty(productParam.getParamValue())) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
    }
}
