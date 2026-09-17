package com.example.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.example.common.enums.ResultCodeEnum;
import com.example.entity.ProductDetail;
import com.example.exception.CustomException;
import com.example.mapper.ProductDetailMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class ProductDetailService {

    @Resource
    private ProductDetailMapper productDetailMapper;

    public void save(ProductDetail productDetail) {
        validate(productDetail);
        ProductDetail dbDetail = productDetailMapper.selectByProductId(productDetail.getProductId());
        if (ObjectUtil.isNull(dbDetail)) {
            String now = DateUtil.now();
            productDetail.setCreateTime(now);
            productDetail.setUpdateTime(now);
            productDetailMapper.insert(productDetail);
        } else {
            productDetail.setId(dbDetail.getId());
            productDetail.setUpdateTime(DateUtil.now());
            productDetailMapper.updateById(productDetail);
        }
    }

    public ProductDetail selectByProductId(Integer productId) {
        if (ObjectUtil.isEmpty(productId)) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        return productDetailMapper.selectByProductId(productId);
    }

    private void validate(ProductDetail productDetail) {
        if (ObjectUtil.isEmpty(productDetail.getProductId())
                || ObjectUtil.isEmpty(productDetail.getDetailContent())) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
    }
}
