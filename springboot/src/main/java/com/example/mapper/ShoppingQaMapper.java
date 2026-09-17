package com.example.mapper;

import com.example.entity.ShoppingQa;
import java.util.List;

public interface ShoppingQaMapper {

    int insert(ShoppingQa shoppingQa);

    void deleteById(Integer id);

    List<ShoppingQa> selectAll(ShoppingQa shoppingQa);
}
