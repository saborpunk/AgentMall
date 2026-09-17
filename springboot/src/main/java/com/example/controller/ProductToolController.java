package com.example.controller;

import com.example.common.Result;
import com.example.entity.ProductToolRequest;
import com.example.entity.ProductToolResult;
import com.example.service.ProductToolService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/productTool")
public class ProductToolController {

    @Resource
    private ProductToolService productToolService;

    @PostMapping("/price")
    public Result price(@RequestBody ProductToolRequest request) {
        ProductToolResult result = productToolService.queryProductPrice(request);
        return Result.success(result);
    }

    @PostMapping("/stock")
    public Result stock(@RequestBody ProductToolRequest request) {
        ProductToolResult result = productToolService.queryProductStock(request);
        return Result.success(result);
    }

    @PostMapping("/promotion")
    public Result promotion(@RequestBody ProductToolRequest request) {
        ProductToolResult result = productToolService.queryProductPromotion(request);
        return Result.success(result);
    }
}
