package com.example.mapper;

import com.example.entity.ShopOrder;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ShopOrderMapper {

    int insert(ShopOrder shopOrder);

    void updateById(ShopOrder shopOrder);

    @Select("select * from shop_order where id = #{id}")
    ShopOrder selectById(Integer id);

    List<ShopOrder> selectAll(ShopOrder shopOrder);
}
