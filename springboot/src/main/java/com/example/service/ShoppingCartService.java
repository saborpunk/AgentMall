package com.example.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.example.common.enums.ResultCodeEnum;
import com.example.entity.Product;
import com.example.entity.ShoppingCart;
import com.example.exception.CustomException;
import com.example.mapper.ProductMapper;
import com.example.mapper.ShoppingCartMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ShoppingCartService {

    @Resource
    private ShoppingCartMapper shoppingCartMapper;
    @Resource
    private ProductMapper productMapper;

    public void add(ShoppingCart shoppingCart) {
        validate(shoppingCart);
        Product product = productMapper.selectById(shoppingCart.getProductId());
        if (ObjectUtil.isNull(product)) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        ShoppingCart dbCart = shoppingCartMapper.selectByUserIdAndProductId(shoppingCart.getUserId(), shoppingCart.getProductId());
        if (ObjectUtil.isNotNull(dbCart)) {
            dbCart.setQuantity(dbCart.getQuantity() + shoppingCart.getQuantity());
            dbCart.setSelected(1);
            dbCart.setUpdateTime(DateUtil.now());
            shoppingCartMapper.updateById(dbCart);
            return;
        }
        shoppingCart.setSelected(ObjectUtil.isEmpty(shoppingCart.getSelected()) ? 1 : shoppingCart.getSelected());
        String now = DateUtil.now();
        shoppingCart.setCreateTime(now);
        shoppingCart.setUpdateTime(now);
        shoppingCartMapper.insert(shoppingCart);
    }

    public void updateById(ShoppingCart shoppingCart) {
        if (ObjectUtil.isEmpty(shoppingCart.getId()) || ObjectUtil.isEmpty(shoppingCart.getQuantity()) || shoppingCart.getQuantity() <= 0) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        shoppingCart.setUpdateTime(DateUtil.now());
        shoppingCartMapper.updateById(shoppingCart);
    }

    public void deleteById(Integer id) {
        shoppingCartMapper.deleteById(id);
    }

    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            shoppingCartMapper.deleteById(id);
        }
    }

    public List<ShoppingCart> selectAll(ShoppingCart shoppingCart) {
        return shoppingCartMapper.selectAll(shoppingCart);
    }

    private void validate(ShoppingCart shoppingCart) {
        if (ObjectUtil.isEmpty(shoppingCart.getUserId())
                || ObjectUtil.isEmpty(shoppingCart.getProductId())
                || ObjectUtil.isEmpty(shoppingCart.getQuantity())
                || shoppingCart.getQuantity() <= 0) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
    }
}
