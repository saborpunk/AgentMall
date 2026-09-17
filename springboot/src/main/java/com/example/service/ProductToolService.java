package com.example.service;

import cn.hutool.core.util.ObjectUtil;
import com.example.common.enums.ResultCodeEnum;
import com.example.entity.Product;
import com.example.entity.ProductToolRequest;
import com.example.entity.ProductToolResult;
import com.example.exception.CustomException;
import com.example.mapper.ProductMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class ProductToolService {

    @Resource
    private ProductMapper productMapper;

    public ProductToolResult queryProductPrice(ProductToolRequest request) {
        Product product = resolveProduct(request);
        ProductToolResult result = buildBaseResult("PRODUCT_PRICE_QUERY", product, request);
        result.setMessage("已查询到商品实时价格");
        return result;
    }

    public ProductToolResult queryProductStock(ProductToolRequest request) {
        Product product = resolveProduct(request);
        ProductToolResult result = buildBaseResult("PRODUCT_STOCK_QUERY", product, request);
        // 库存就存在商品自己的 stock_quantity 字段上，不再查单独的库存表
        fillStock(result, product);
        // 模型没传购买数量时按 1 件判断能不能买
        int quantity = request.getQuantity() == null || request.getQuantity() < 1 ? 1 : request.getQuantity();
        result.setQuantity(quantity);
        result.setCanBuy(result.getStockQuantity() >= quantity ? 1 : 0);
        result.setMessage(result.getCanBuy() == 1 ? "库存充足，可以购买" : "库存不足");
        return result;
    }

    public ProductToolResult queryProductPromotion(ProductToolRequest request) {
        Product product = resolveProduct(request);
        ProductToolResult result = buildBaseResult("PRODUCT_PROMOTION_QUERY", product, request);
        BigDecimal originalPrice = product.getOriginalPrice() == null ? product.getPrice() : product.getOriginalPrice();
        BigDecimal discountAmount = originalPrice.subtract(product.getPrice()).max(BigDecimal.ZERO);
        BigDecimal discountRate = BigDecimal.ZERO;
        if (originalPrice.compareTo(BigDecimal.ZERO) > 0) {
            discountRate = product.getPrice().divide(originalPrice, 4, RoundingMode.HALF_UP);
        }
        result.setDiscountAmount(discountAmount);
        result.setDiscountRate(discountRate);
        result.setMessage(discountAmount.compareTo(BigDecimal.ZERO) > 0 ? "当前商品存在价格优惠" : "当前商品暂无价格优惠");
        return result;
    }

    private Product resolveProduct(ProductToolRequest request) {
        if (ObjectUtil.isNull(request)) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        Product product = null;
        if (ObjectUtil.isNotEmpty(request.getProductId())) {
            product = productMapper.selectById(request.getProductId());
        } else if (ObjectUtil.isNotEmpty(request.getProductNo())) {
            product = productMapper.selectByProductNo(request.getProductNo());
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

    private ProductToolResult buildBaseResult(String toolCode, Product product, ProductToolRequest request) {
        int quantity = request.getQuantity() == null || request.getQuantity() < 1 ? 1 : request.getQuantity();
        BigDecimal totalAmount = product.getPrice().multiply(BigDecimal.valueOf(quantity));
        ProductToolResult result = new ProductToolResult();
        result.setToolCode(toolCode);
        result.setProductId(product.getId());
        result.setProductNo(product.getProductNo());
        result.setProductName(product.getName());
        result.setStatus(product.getStatus());
        result.setPrice(product.getPrice());
        result.setOriginalPrice(product.getOriginalPrice());
        result.setQuantity(quantity);
        result.setTotalAmount(totalAmount);
        result.setCanBuy("ON_SALE".equals(product.getStatus()) ? 1 : 0);
        return result;
    }

    private void fillStock(ProductToolResult result, Product product) {
        // 商品没填库存时当作 0，下游判断可买性时就会直接判不可买
        result.setStockQuantity(product.getStockQuantity() == null ? 0 : product.getStockQuantity());
    }
}
