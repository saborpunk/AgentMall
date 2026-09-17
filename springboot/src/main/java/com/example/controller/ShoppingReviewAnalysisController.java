package com.example.controller;

import com.example.common.Result;
import com.example.entity.ReviewAnalysisRequest;
import com.example.entity.ShoppingReviewAnalysis;
import com.example.service.ShoppingReviewAnalysisService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/shoppingReviewAnalysis")
public class ShoppingReviewAnalysisController {

    @Resource
    private ShoppingReviewAnalysisService shoppingReviewAnalysisService;

    @PostMapping("/generate")
    public Result generate(@RequestBody ReviewAnalysisRequest request) {
        ShoppingReviewAnalysis analysis = shoppingReviewAnalysisService.generate(request);
        return Result.success(analysis);
    }

    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id) {
        shoppingReviewAnalysisService.deleteById(id);
        return Result.success();
    }

    @DeleteMapping("/delete/batch")
    public Result delete(@RequestBody List<Integer> ids) {
        shoppingReviewAnalysisService.deleteBatch(ids);
        return Result.success();
    }

    @GetMapping("/selectPage")
    public Result selectPage(ShoppingReviewAnalysis shoppingReviewAnalysis,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<ShoppingReviewAnalysis> pageInfo = shoppingReviewAnalysisService.selectPage(shoppingReviewAnalysis, pageNum, pageSize);
        return Result.success(pageInfo);
    }
}
