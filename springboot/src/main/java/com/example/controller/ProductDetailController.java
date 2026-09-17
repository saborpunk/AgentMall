package com.example.controller;

import com.example.common.Result;
import com.example.entity.ProductDetail;
import com.example.service.ProductDetailService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/productDetail")
public class ProductDetailController {

    @Resource
    private ProductDetailService productDetailService;

    @PostMapping("/save")
    public Result save(@RequestBody ProductDetail productDetail) {
        productDetailService.save(productDetail);
        return Result.success();
    }

    @GetMapping("/selectByProductId/{productId}")
    public Result selectByProductId(@PathVariable Integer productId) {
        ProductDetail productDetail = productDetailService.selectByProductId(productId);
        return Result.success(productDetail);
    }

}
