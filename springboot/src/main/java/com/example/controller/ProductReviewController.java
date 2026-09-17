package com.example.controller;

import com.example.common.Result;
import com.example.entity.ProductReview;
import com.example.service.ProductReviewService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/productReview")
public class ProductReviewController {

    @Resource
    private ProductReviewService productReviewService;

    @PostMapping("/add")
    public Result add(@RequestBody ProductReview productReview) {
        productReviewService.add(productReview);
        return Result.success();
    }

    @PutMapping("/audit")
    public Result audit(@RequestBody ProductReview productReview) {
        productReviewService.audit(productReview);
        return Result.success();
    }

    @GetMapping("/selectAll")
    public Result selectAll(ProductReview productReview) {
        List<ProductReview> list = productReviewService.selectAll(productReview);
        return Result.success(list);
    }

    @GetMapping("/selectPage")
    public Result selectPage(ProductReview productReview,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<ProductReview> pageInfo = productReviewService.selectPage(productReview, pageNum, pageSize);
        return Result.success(pageInfo);
    }
}
