package com.example.controller;

import com.example.common.Result;
import com.example.entity.GrowthReportRequest;
import com.example.entity.ShoppingGrowthReport;
import com.example.service.ShoppingGrowthReportService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/shoppingGrowthReport")
public class ShoppingGrowthReportController {

    @Resource
    private ShoppingGrowthReportService shoppingGrowthReportService;

    @PostMapping("/generate")
    public Result generate(@RequestBody GrowthReportRequest request) {
        ShoppingGrowthReport report = shoppingGrowthReportService.generate(request);
        return Result.success(report);
    }

    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id) {
        shoppingGrowthReportService.deleteById(id);
        return Result.success();
    }

    @DeleteMapping("/delete/batch")
    public Result delete(@RequestBody List<Integer> ids) {
        shoppingGrowthReportService.deleteBatch(ids);
        return Result.success();
    }

    @GetMapping("/selectPage")
    public Result selectPage(ShoppingGrowthReport shoppingGrowthReport,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<ShoppingGrowthReport> pageInfo = shoppingGrowthReportService.selectPage(shoppingGrowthReport, pageNum, pageSize);
        return Result.success(pageInfo);
    }
}
