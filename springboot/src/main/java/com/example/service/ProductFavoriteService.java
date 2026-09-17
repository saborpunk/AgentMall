package com.example.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.example.common.enums.ResultCodeEnum;
import com.example.entity.ProductFavorite;
import com.example.entity.Product;
import com.example.exception.CustomException;
import com.example.mapper.ProductFavoriteMapper;
import com.example.mapper.ProductMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductFavoriteService {

    @Resource
    private ProductFavoriteMapper productFavoriteMapper;
    @Resource
    private ProductMapper productMapper;

    public void add(ProductFavorite productFavorite) {
        validate(productFavorite);
        Product product = productMapper.selectById(productFavorite.getProductId());
        if (ObjectUtil.isNull(product)) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        ProductFavorite dbFavorite = productFavoriteMapper.selectByUserIdAndProductId(productFavorite.getUserId(), productFavorite.getProductId());
        if (ObjectUtil.isNotNull(dbFavorite)) {
            return;
        }
        productFavorite.setCreateTime(DateUtil.now());
        productFavoriteMapper.insert(productFavorite);
    }

    public void deleteById(Integer id) {
        productFavoriteMapper.deleteById(id);
    }

    public List<ProductFavorite> selectAll(ProductFavorite productFavorite) {
        return productFavoriteMapper.selectAll(productFavorite);
    }

    private void validate(ProductFavorite productFavorite) {
        if (ObjectUtil.isEmpty(productFavorite.getUserId()) || ObjectUtil.isEmpty(productFavorite.getProductId())) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
    }
}
