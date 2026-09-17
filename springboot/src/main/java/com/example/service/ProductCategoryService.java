package com.example.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.example.common.enums.ResultCodeEnum;
import com.example.entity.ProductCategory;
import com.example.exception.CustomException;
import com.example.mapper.ProductCategoryMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductCategoryService {

    @Resource
    private ProductCategoryMapper productCategoryMapper;

    public void add(ProductCategory productCategory) {
        validate(productCategory);
        if (productCategory.getSort() == null) {
            productCategory.setSort(1);
        }
        if (productCategory.getIsEnabled() == null) {
            productCategory.setIsEnabled(1);
        }
        String now = DateUtil.now();
        productCategory.setCreateTime(now);
        productCategory.setUpdateTime(now);
        productCategoryMapper.insert(productCategory);
    }

    public void updateById(ProductCategory productCategory) {
        if (ObjectUtil.isEmpty(productCategory.getId())) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        validate(productCategory);
        productCategory.setUpdateTime(DateUtil.now());
        productCategoryMapper.updateById(productCategory);
    }

    public void deleteById(Integer id) {
        productCategoryMapper.deleteById(id);
    }

    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            deleteById(id);
        }
    }

    public List<ProductCategory> selectAll(ProductCategory productCategory) {
        return productCategoryMapper.selectAll(productCategory);
    }

    public PageInfo<ProductCategory> selectPage(ProductCategory productCategory, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<ProductCategory> list = productCategoryMapper.selectAll(productCategory);
        return PageInfo.of(list);
    }

    private void validate(ProductCategory productCategory) {
        if (ObjectUtil.isEmpty(productCategory.getName())) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
    }
}
