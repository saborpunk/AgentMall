package com.example.mapper;

import com.example.entity.ShoppingReviewAnalysis;
import java.util.List;

public interface ShoppingReviewAnalysisMapper {

    int insert(ShoppingReviewAnalysis shoppingReviewAnalysis);

    void deleteById(Integer id);

    void deleteByProductId(Integer productId);

    List<ShoppingReviewAnalysis> selectAll(ShoppingReviewAnalysis shoppingReviewAnalysis);
}
