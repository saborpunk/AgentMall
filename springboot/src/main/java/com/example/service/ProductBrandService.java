package com.example.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.example.common.enums.ResultCodeEnum;
import com.example.entity.ProductBrand;
import com.example.exception.CustomException;
import com.example.mapper.ProductBrandMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductBrandService {

    @Resource
    private ProductBrandMapper productBrandMapper;

    public void add(ProductBrand productBrand) {
        validate(productBrand);
        if (productBrand.getSort() == null) {
            productBrand.setSort(1);
        }
        if (productBrand.getIsEnabled() == null) {
            productBrand.setIsEnabled(1);
        }
        String now = DateUtil.now();
        productBrand.setCreateTime(now);
        productBrand.setUpdateTime(now);
        productBrandMapper.insert(productBrand);
    }

    public void updateById(ProductBrand productBrand) {
        if (ObjectUtil.isEmpty(productBrand.getId())) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        validate(productBrand);
        productBrand.setUpdateTime(DateUtil.now());
        productBrandMapper.updateById(productBrand);
    }

    public void deleteById(Integer id) {
        productBrandMapper.deleteById(id);
    }

    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            productBrandMapper.deleteById(id);
        }
    }

    public List<ProductBrand> selectAll(ProductBrand productBrand) {
        return productBrandMapper.selectAll(productBrand);
    }

    public PageInfo<ProductBrand> selectPage(ProductBrand productBrand, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<ProductBrand> list = productBrandMapper.selectAll(productBrand);
        return PageInfo.of(list);
    }

    private void validate(ProductBrand productBrand) {
        if (ObjectUtil.isEmpty(productBrand.getName())) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
    }
}
