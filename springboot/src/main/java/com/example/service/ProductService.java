package com.example.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.example.common.enums.ResultCodeEnum;
import com.example.entity.Product;
import com.example.exception.CustomException;
import com.example.mapper.ProductMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ProductService {

    @Resource
    private ProductMapper productMapper;

    public void add(Product product) {
        validate(product);
        if (ObjectUtil.isEmpty(product.getProductNo())) {
            product.setProductNo("SP" + DateUtil.format(DateUtil.date(), "yyyyMMddHHmmssSSS"));
        }
        Product dbProduct = productMapper.selectByProductNo(product.getProductNo());
        if (ObjectUtil.isNotNull(dbProduct)) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        setDefaultValue(product);
        String now = DateUtil.now();
        product.setCreateTime(now);
        product.setUpdateTime(now);
        productMapper.insert(product);
    }

    public void updateById(Product product) {
        if (ObjectUtil.isEmpty(product.getId())) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        validate(product);
        Product dbProduct = productMapper.selectByProductNo(product.getProductNo());
        if (ObjectUtil.isNotNull(dbProduct) && !dbProduct.getId().equals(product.getId())) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        setDefaultValue(product);
        product.setUpdateTime(DateUtil.now());
        productMapper.updateById(product);
    }

    public void deleteById(Integer id) {
        productMapper.deleteById(id);
    }

    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            productMapper.deleteById(id);
        }
    }

    public List<Product> selectAll(Product product) {
        return productMapper.selectAll(product);
    }

    public PageInfo<Product> selectPage(Product product, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Product> list = productMapper.selectAll(product);
        return PageInfo.of(list);
    }

    private void validate(Product product) {
        if (ObjectUtil.isEmpty(product.getName())
                || ObjectUtil.isEmpty(product.getCategoryId())
                || ObjectUtil.isEmpty(product.getBrandId())
                || product.getPrice() == null
                || product.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
    }

    private void setDefaultValue(Product product) {
        if (product.getOriginalPrice() == null) {
            product.setOriginalPrice(product.getPrice());
        }
        if (product.getIsRecommend() == null) {
            product.setIsRecommend(0);
        }
        if (ObjectUtil.isEmpty(product.getStatus())) {
            product.setStatus("ON_SALE");
        }
        if (product.getSort() == null) {
            product.setSort(1);
        }
    }
}
