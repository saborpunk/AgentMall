package com.example.service;

import cn.hutool.core.util.ObjectUtil;
import com.example.common.enums.ResultCodeEnum;
import com.example.entity.BusinessToolRequest;
import com.example.entity.BusinessToolResult;
import com.example.entity.Product;
import com.example.entity.ShopOrder;
import com.example.entity.User;
import com.example.exception.CustomException;
import com.example.mapper.ProductMapper;
import com.example.mapper.ShopOrderMapper;
import com.example.mapper.UserMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class BusinessToolService {

    @Resource
    private UserMapper userMapper;
    @Resource
    private ProductMapper productMapper;
    @Resource
    private ShopOrderMapper shopOrderMapper;

    public BusinessToolResult queryUserProfile(BusinessToolRequest request) {
        User user = resolveUser(request);
        ShopOrder condition = new ShopOrder();
        condition.setUserId(user.getId());
        List<ShopOrder> orders = shopOrderMapper.selectAll(condition);
        BigDecimal orderAmount = BigDecimal.ZERO;
        for (ShopOrder order : orders) {
            if (order.getTotalAmount() != null) {
                orderAmount = orderAmount.add(order.getTotalAmount());
            }
        }
        BusinessToolResult result = new BusinessToolResult();
        result.setToolCode("USER_PROFILE_QUERY");
        result.setUserId(user.getId());
        result.setUsername(user.getUsername());
        result.setUserName(user.getName());
        result.setPhone(user.getPhone());
        result.setEmail(user.getEmail());
        result.setBalance(user.getBalance());
        result.setOrderCount(orders.size());
        result.setOrderAmount(orderAmount);
        if (!orders.isEmpty()) {
            result.setLatestOrderNo(orders.get(0).getOrderNo());
            result.setLatestOrderStatus(orders.get(0).getStatus());
        }
        result.setMessage("已查询到用户画像信息");
        return result;
    }

    public BusinessToolResult querySimilarProducts(BusinessToolRequest request) {
        Product product = resolveProduct(request);
        int limit = request.getLimit() == null || request.getLimit() < 1 ? 5 : Math.min(request.getLimit(), 20);
        List<Product> similarProducts = new ArrayList<>();
        Product categoryCondition = new Product();
        categoryCondition.setCategoryId(product.getCategoryId());
        categoryCondition.setStatus("ON_SALE");
        for (Product item : productMapper.selectAll(categoryCondition)) {
            if (!item.getId().equals(product.getId()) && similarProducts.size() < limit) {
                similarProducts.add(item);
            }
        }
        if (similarProducts.size() < limit && product.getBrandId() != null) {
            Product brandCondition = new Product();
            brandCondition.setBrandId(product.getBrandId());
            brandCondition.setStatus("ON_SALE");
            for (Product item : productMapper.selectAll(brandCondition)) {
                if (!item.getId().equals(product.getId())
                        && similarProducts.stream().noneMatch(existing -> existing.getId().equals(item.getId()))
                        && similarProducts.size() < limit) {
                    similarProducts.add(item);
                }
            }
        }
        BusinessToolResult result = new BusinessToolResult();
        result.setToolCode("SIMILAR_PRODUCT_QUERY");
        result.setProductId(product.getId());
        result.setProductNo(product.getProductNo());
        result.setProductName(product.getName());
        result.setCategoryId(product.getCategoryId());
        result.setBrandId(product.getBrandId());
        result.setSimilarProducts(similarProducts);
        result.setMessage("已根据分类和品牌召回相似商品");
        return result;
    }

    public BusinessToolResult queryOrderStatus(BusinessToolRequest request) {
        ShopOrder order = resolveOrder(request);
        BusinessToolResult result = new BusinessToolResult();
        result.setToolCode("ORDER_STATUS_QUERY");
        result.setOrderId(order.getId());
        result.setOrderNo(order.getOrderNo());
        result.setUserId(order.getUserId());
        result.setUserName(order.getUserName());
        result.setOrderStatus(order.getStatus());
        result.setTotalAmount(order.getTotalAmount());
        result.setTotalQuantity(order.getTotalQuantity());
        result.setReceiverName(order.getReceiverName());
        result.setReceiverPhone(order.getReceiverPhone());
        result.setReceiverAddress(order.getReceiverAddress());
        result.setPayTime(order.getPayTime());
        result.setShipTime(order.getShipTime());
        result.setFinishTime(order.getFinishTime());
        result.setCancelTime(order.getCancelTime());
        result.setMessage("已查询到订单状态");
        return result;
    }

    private User resolveUser(BusinessToolRequest request) {
        if (ObjectUtil.isNull(request)) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        User user = null;
        if (ObjectUtil.isNotEmpty(request.getUserId())) {
            user = userMapper.selectById(request.getUserId());
        } else if (ObjectUtil.isNotEmpty(request.getUsername())) {
            user = userMapper.selectByUsername(request.getUsername());
        }
        if (ObjectUtil.isNull(user)) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        return user;
    }

    private Product resolveProduct(BusinessToolRequest request) {
        if (ObjectUtil.isNull(request)) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        Product product = null;
        if (ObjectUtil.isNotEmpty(request.getProductId())) {
            product = productMapper.selectById(request.getProductId());
        } else if (ObjectUtil.isNotEmpty(request.getProductName())) {
            Product condition = new Product();
            condition.setName(request.getProductName());
            List<Product> products = productMapper.selectAll(condition);
            if (!products.isEmpty()) {
                product = products.get(0);
            }
        }
        if (ObjectUtil.isNull(product)) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        return product;
    }

    private ShopOrder resolveOrder(BusinessToolRequest request) {
        if (ObjectUtil.isNull(request)) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        ShopOrder order = null;
        if (ObjectUtil.isNotEmpty(request.getOrderId())) {
            order = shopOrderMapper.selectById(request.getOrderId());
        } else if (ObjectUtil.isNotEmpty(request.getOrderNo())) {
            ShopOrder condition = new ShopOrder();
            condition.setOrderNo(request.getOrderNo());
            List<ShopOrder> orders = shopOrderMapper.selectAll(condition);
            if (!orders.isEmpty()) {
                order = orders.get(0);
            }
        }
        if (ObjectUtil.isNull(order)) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        return order;
    }
}
