package com.example.mapper;

import com.example.entity.ProductParam;
import java.util.List;

public interface ProductParamMapper {

    int insert(ProductParam productParam);

    void updateById(ProductParam productParam);

    void deleteById(Integer id);

    List<ProductParam> selectAll(ProductParam productParam);
}
