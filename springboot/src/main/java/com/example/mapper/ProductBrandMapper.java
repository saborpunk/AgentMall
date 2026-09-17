package com.example.mapper;

import com.example.entity.ProductBrand;
import java.util.List;

public interface ProductBrandMapper {

    int insert(ProductBrand productBrand);

    void updateById(ProductBrand productBrand);

    void deleteById(Integer id);

    List<ProductBrand> selectAll(ProductBrand productBrand);
}
