package com.example.mapper;

import com.example.entity.ProductCategory;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ProductCategoryMapper {

    int insert(ProductCategory productCategory);

    void updateById(ProductCategory productCategory);

    void deleteById(Integer id);

    List<ProductCategory> selectAll(ProductCategory productCategory);
}
