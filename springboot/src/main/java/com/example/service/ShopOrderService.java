package com.example.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.example.common.enums.ResultCodeEnum;
import com.example.entity.*;
import com.example.exception.CustomException;
import com.example.mapper.*;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ShopOrderService {

    @Resource
    private ShopOrderMapper shopOrderMapper;
    @Resource
    private ShopOrderItemMapper shopOrderItemMapper;
    @Resource
    private ShoppingCartMapper shoppingCartMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private WalletRecordMapper walletRecordMapper;
    // 库存直接存在 product.stock_quantity 上，下单扣减和取消回补都通过它更新
    @Resource
    private ProductMapper productMapper;

    @Transactional
    public ShopOrder create(OrderCreateRequest request) {
        validateCreateRequest(request);
        User user = userMapper.selectById(request.getUserId());
        if (ObjectUtil.isNull(user)) {
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        ShoppingCart query = new ShoppingCart();
        query.setUserId(request.getUserId());
        List<ShoppingCart> cartList = shoppingCartMapper.selectAll(query)
                .stream()
                .filter(item -> item.getSelected() != null && item.getSelected() == 1)
                .toList();
        if (cartList.isEmpty()) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalQuantity = 0;
        for (ShoppingCart cart : cartList) {
            validateCartItem(cart);
            totalAmount = totalAmount.add(cart.getPrice().multiply(BigDecimal.valueOf(cart.getQuantity())));
            totalQuantity += cart.getQuantity();
        }

        int decreaseResult = userMapper.decreaseBalance(request.getUserId(), totalAmount);
        if (decreaseResult == 0) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        User admin = userMapper.selectFirstAdmin();
        if (ObjectUtil.isNull(admin)) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        userMapper.increaseBalance(admin.getId(), totalAmount);

        String now = DateUtil.now();
        String orderNo = "OD" + DateUtil.format(DateUtil.date(), "yyyyMMddHHmmssSSS");
        ShopOrder order = new ShopOrder();
        order.setOrderNo(orderNo);
        order.setUserId(request.getUserId());
        order.setTotalAmount(totalAmount);
        order.setTotalQuantity(totalQuantity);
        order.setStatus("PAID");
        order.setReceiverName(request.getReceiverName());
        order.setReceiverPhone(request.getReceiverPhone());
        order.setReceiverAddress(request.getReceiverAddress());
        order.setPayTime(now);
        order.setCreateTime(now);
        order.setUpdateTime(now);
        // insert 配了 keyProperty="id"，插完自增主键会回填到 order 对象上，
        // 下面写订单明细、回查订单都要用这个 order.getId()。
        shopOrderMapper.insert(order);

        for (ShoppingCart cart : cartList) {
            // 支付成功直接扣减商品库存
            reduceProductStock(cart.getProductId(), cart.getQuantity());

            ShopOrderItem orderItem = new ShopOrderItem();
            // order_id 是 shop_order_item 的非空外键，取上面刚插入的订单主表自增 ID
            orderItem.setOrderId(order.getId());
            orderItem.setProductId(cart.getProductId());
            orderItem.setProductNo(cart.getProductNo());
            orderItem.setProductName(cart.getProductName());
            orderItem.setCoverImage(cart.getCoverImage());
            orderItem.setPrice(cart.getPrice());
            orderItem.setQuantity(cart.getQuantity());
            orderItem.setSubtotalAmount(cart.getPrice().multiply(BigDecimal.valueOf(cart.getQuantity())));
            orderItem.setCreateTime(now);
            shopOrderItemMapper.insert(orderItem);
            shoppingCartMapper.deleteById(cart.getId());
        }

        addWalletRecord(request.getUserId(), "PAY", totalAmount.negate(), orderNo, "订单支付");
        addWalletRecord(admin.getId(), "INCOME", totalAmount, orderNo, "订单收入");
        ShopOrder created = shopOrderMapper.selectById(order.getId());
        created.setItems(shopOrderItemMapper.selectByOrderId(order.getId()));
        return created;
    }

    public List<ShopOrder> selectAll(ShopOrder shopOrder) {
        List<ShopOrder> list = shopOrderMapper.selectAll(shopOrder);
        for (ShopOrder order : list) {
            order.setItems(shopOrderItemMapper.selectByOrderId(order.getId()));
        }
        return list;
    }

    public PageInfo<ShopOrder> selectPage(ShopOrder shopOrder, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<ShopOrder> list = shopOrderMapper.selectAll(shopOrder);
        for (ShopOrder order : list) {
            order.setItems(shopOrderItemMapper.selectByOrderId(order.getId()));
        }
        return PageInfo.of(list);
    }

    public void ship(Integer id) {
        ShopOrder order = getOrder(id);
        if (!"PAID".equals(order.getStatus())) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        ShopOrder update = new ShopOrder();
        update.setId(id);
        update.setStatus("SHIPPED");
        update.setShipTime(DateUtil.now());
        update.setUpdateTime(DateUtil.now());
        shopOrderMapper.updateById(update);
    }

    public void finish(Integer id) {
        ShopOrder order = getOrder(id);
        if (!"SHIPPED".equals(order.getStatus())) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        ShopOrder update = new ShopOrder();
        update.setId(id);
        update.setStatus("COMPLETED");
        update.setFinishTime(DateUtil.now());
        update.setUpdateTime(DateUtil.now());
        shopOrderMapper.updateById(update);
    }

    @Transactional
    public void cancel(Integer id) {
        ShopOrder order = getOrder(id);
        if (!"PAID".equals(order.getStatus())) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        User admin = userMapper.selectFirstAdmin();
        if (ObjectUtil.isNull(admin)) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        int decreaseResult = userMapper.decreaseBalance(admin.getId(), order.getTotalAmount());
        if (decreaseResult == 0) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        userMapper.increaseBalance(order.getUserId(), order.getTotalAmount());

        List<ShopOrderItem> items = shopOrderItemMapper.selectByOrderId(id);
        for (ShopOrderItem item : items) {
            // 取消订单把已扣的库存加回去
            increaseProductStock(item.getProductId(), item.getQuantity());
        }

        String now = DateUtil.now();
        ShopOrder update = new ShopOrder();
        update.setId(id);
        update.setStatus("CANCELLED");
        update.setCancelTime(now);
        update.setUpdateTime(now);
        shopOrderMapper.updateById(update);
        addWalletRecord(order.getUserId(), "REFUND", order.getTotalAmount(), order.getOrderNo(), "订单取消退款");
        addWalletRecord(admin.getId(), "REFUND_OUT", order.getTotalAmount().negate(), order.getOrderNo(), "订单取消退款支出");
    }

    private ShopOrder getOrder(Integer id) {
        if (ObjectUtil.isEmpty(id)) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        ShopOrder order = shopOrderMapper.selectById(id);
        if (ObjectUtil.isNull(order)) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        return order;
    }

    private void validateCreateRequest(OrderCreateRequest request) {
        if (ObjectUtil.isEmpty(request.getUserId())
                || ObjectUtil.isEmpty(request.getReceiverName())
                || ObjectUtil.isEmpty(request.getReceiverPhone())
                || ObjectUtil.isEmpty(request.getReceiverAddress())) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
    }

    private void validateCartItem(ShoppingCart cart) {
        if (!"ON_SALE".equals(cart.getStatus())
                || cart.getPrice() == null
                || ObjectUtil.isEmpty(cart.getQuantity())
                || cart.getQuantity() <= 0) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        // 库存存在 product.stock_quantity 上，购物车列表查询时联表带出来
        int stockQuantity = cart.getStockQuantity() == null ? 0 : cart.getStockQuantity();
        if (stockQuantity < cart.getQuantity()) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
    }

    /**
     * 下单支付成功后扣库存：直接把 product.stock_quantity 减掉对应数量，
     * 库存不够时报错，避免出现负库存
     */
    private void reduceProductStock(Integer productId, Integer quantity) {
        Product product = productMapper.selectById(productId);
        if (ObjectUtil.isNull(product)) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        int stockQuantity = product.getStockQuantity() == null ? 0 : product.getStockQuantity();
        if (stockQuantity < quantity) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        Product update = new Product();
        update.setId(productId);
        update.setStockQuantity(stockQuantity - quantity);
        update.setUpdateTime(DateUtil.now());
        productMapper.updateById(update);
    }

    /**
     * 取消订单时回补库存：把之前扣掉的数量加回 product.stock_quantity
     */
    private void increaseProductStock(Integer productId, Integer quantity) {
        Product product = productMapper.selectById(productId);
        if (ObjectUtil.isNull(product)) {
            return;
        }
        int stockQuantity = product.getStockQuantity() == null ? 0 : product.getStockQuantity();
        Product update = new Product();
        update.setId(productId);
        update.setStockQuantity(stockQuantity + quantity);
        update.setUpdateTime(DateUtil.now());
        productMapper.updateById(update);
    }

    private void addWalletRecord(Integer userId, String type, BigDecimal amount, String businessNo, String remark) {
        User updatedUser = userMapper.selectById(userId);
        WalletRecord walletRecord = new WalletRecord();
        walletRecord.setUserId(userId);
        walletRecord.setType(type);
        walletRecord.setAmount(amount);
        walletRecord.setBalanceAfter(updatedUser.getBalance());
        walletRecord.setBusinessNo(businessNo);
        walletRecord.setRemark(remark);
        walletRecordMapper.insert(walletRecord);
    }
}
