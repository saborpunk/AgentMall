package com.example.mapper;

import com.example.entity.ProductReview;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ProductReviewMapper {

    int insert(ProductReview productReview);

    void updateById(ProductReview productReview);

    @Select("select * from product_review where order_item_id = #{orderItemId}")
    ProductReview selectByOrderItemId(Integer orderItemId);

    List<ProductReview> selectAll(ProductReview productReview);
}
