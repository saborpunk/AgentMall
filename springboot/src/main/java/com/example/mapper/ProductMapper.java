package com.example.mapper;

import com.example.entity.Product;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ProductMapper {

    int insert(Product product);

    void updateById(Product product);

    void deleteById(Integer id);

    @Select("select * from product where id = #{id}")
    Product selectById(Integer id);

    @Select("select * from product where product_no = #{productNo}")
    Product selectByProductNo(String productNo);

    List<Product> selectAll(Product product);
}
