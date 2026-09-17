package com.example.mapper;

import com.example.entity.ShoppingCart;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ShoppingCartMapper {

    int insert(ShoppingCart shoppingCart);

    void updateById(ShoppingCart shoppingCart);

    void deleteById(Integer id);

    @Select("select * from shopping_cart where user_id = #{userId} and product_id = #{productId}")
    ShoppingCart selectByUserIdAndProductId(@Param("userId") Integer userId, @Param("productId") Integer productId);

    List<ShoppingCart> selectAll(ShoppingCart shoppingCart);
}
