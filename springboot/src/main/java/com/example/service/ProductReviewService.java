package com.example.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.example.common.enums.ResultCodeEnum;
import com.example.entity.ProductReview;
import com.example.entity.ShopOrder;
import com.example.entity.ShopOrderItem;
import com.example.exception.CustomException;
import com.example.mapper.ProductReviewMapper;
import com.example.mapper.ShopOrderItemMapper;
import com.example.mapper.ShopOrderMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductReviewService {

    @Resource
    private ProductReviewMapper productReviewMapper;
    @Resource
    private ShopOrderMapper shopOrderMapper;
    @Resource
    private ShopOrderItemMapper shopOrderItemMapper;

    public void add(ProductReview productReview) {
        validate(productReview);
        ShopOrder order = shopOrderMapper.selectById(productReview.getOrderId());
        if (ObjectUtil.isNull(order)
                || !productReview.getUserId().equals(order.getUserId())
                || !"COMPLETED".equals(order.getStatus())) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        ShopOrderItem orderItem = shopOrderItemMapper.selectById(productReview.getOrderItemId());
        if (ObjectUtil.isNull(orderItem)
                || !productReview.getOrderId().equals(orderItem.getOrderId())
                || !productReview.getProductId().equals(orderItem.getProductId())) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        ProductReview dbReview = productReviewMapper.selectByOrderItemId(productReview.getOrderItemId());
        if (ObjectUtil.isNotNull(dbReview)) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        productReview.setAuditStatus("APPROVED");
        String now = DateUtil.now();
        productReview.setCreateTime(now);
        productReview.setUpdateTime(now);
        productReviewMapper.insert(productReview);
    }

    public void audit(ProductReview productReview) {
        if (ObjectUtil.isEmpty(productReview.getId()) || ObjectUtil.isEmpty(productReview.getAuditStatus())) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        productReview.setUpdateTime(DateUtil.now());
        productReviewMapper.updateById(productReview);
    }

    public List<ProductReview> selectAll(ProductReview productReview) {
        return productReviewMapper.selectAll(productReview);
    }

    public PageInfo<ProductReview> selectPage(ProductReview productReview, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<ProductReview> list = productReviewMapper.selectAll(productReview);
        return PageInfo.of(list);
    }

    private void validate(ProductReview productReview) {
        if (ObjectUtil.isEmpty(productReview.getUserId())
                || ObjectUtil.isEmpty(productReview.getOrderId())
                || ObjectUtil.isEmpty(productReview.getOrderItemId())
                || ObjectUtil.isEmpty(productReview.getProductId())
                || ObjectUtil.isEmpty(productReview.getContent())
                || productReview.getRating() == null
                || productReview.getRating() < 1
                || productReview.getRating() > 5) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
    }
}
