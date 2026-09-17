package com.example.controller;

import com.example.common.Result;
import com.example.entity.ShoppingRecommendation;
import com.example.service.ShoppingRecommendationService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/shoppingRecommendation")
public class ShoppingRecommendationController {

    @Resource
    private ShoppingRecommendationService shoppingRecommendationService;

    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id) {
        shoppingRecommendationService.deleteById(id);
        return Result.success();
    }

    @DeleteMapping("/delete/batch")
    public Result delete(@RequestBody List<Integer> ids) {
        shoppingRecommendationService.deleteBatch(ids);
        return Result.success();
    }

    @GetMapping("/selectAll")
    public Result selectAll(ShoppingRecommendation shoppingRecommendation) {
        List<ShoppingRecommendation> list = shoppingRecommendationService.selectAll(shoppingRecommendation);
        return Result.success(list);
    }

    @GetMapping("/selectPage")
    public Result selectPage(ShoppingRecommendation shoppingRecommendation,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<ShoppingRecommendation> pageInfo = shoppingRecommendationService.selectPage(shoppingRecommendation, pageNum, pageSize);
        return Result.success(pageInfo);
    }
}
