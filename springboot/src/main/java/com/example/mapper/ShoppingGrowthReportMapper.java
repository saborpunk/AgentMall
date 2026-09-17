package com.example.mapper;

import com.example.entity.ShoppingGrowthReport;
import java.util.List;

public interface ShoppingGrowthReportMapper {

    int insert(ShoppingGrowthReport shoppingGrowthReport);

    void deleteById(Integer id);

    List<ShoppingGrowthReport> selectAll(ShoppingGrowthReport shoppingGrowthReport);
}
