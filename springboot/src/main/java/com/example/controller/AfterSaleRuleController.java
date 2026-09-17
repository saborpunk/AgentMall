package com.example.controller;

import com.example.common.Result;
import com.example.entity.AfterSaleRule;
import com.example.service.AfterSaleRuleService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/afterSaleRule")
public class AfterSaleRuleController {

    @Resource
    private AfterSaleRuleService afterSaleRuleService;

    @PostMapping("/add")
    public Result add(@RequestBody AfterSaleRule afterSaleRule) {
        afterSaleRuleService.add(afterSaleRule);
        return Result.success();
    }

    @PutMapping("/update")
    public Result update(@RequestBody AfterSaleRule afterSaleRule) {
        afterSaleRuleService.updateById(afterSaleRule);
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id) {
        afterSaleRuleService.deleteById(id);
        return Result.success();
    }

    @DeleteMapping("/delete/batch")
    public Result delete(@RequestBody List<Integer> ids) {
        afterSaleRuleService.deleteBatch(ids);
        return Result.success();
    }

    @GetMapping("/selectPage")
    public Result selectPage(AfterSaleRule afterSaleRule,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<AfterSaleRule> pageInfo = afterSaleRuleService.selectPage(afterSaleRule, pageNum, pageSize);
        return Result.success(pageInfo);
    }
}
