package com.example.controller;

import com.example.common.Result;
import com.example.entity.ShoppingGuideTask;
import com.example.service.ShoppingGuideTaskService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/shoppingGuideTask")
public class ShoppingGuideTaskController {

    @Resource
    private ShoppingGuideTaskService shoppingGuideTaskService;

    @PostMapping("/add")
    public Result add(@RequestBody ShoppingGuideTask shoppingGuideTask) {
        shoppingGuideTaskService.add(shoppingGuideTask);
        return Result.success(shoppingGuideTask);
    }

    @PutMapping("/update")
    public Result update(@RequestBody ShoppingGuideTask shoppingGuideTask) {
        shoppingGuideTaskService.updateById(shoppingGuideTask);
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id) {
        shoppingGuideTaskService.deleteById(id);
        return Result.success();
    }

    @DeleteMapping("/delete/batch")
    public Result delete(@RequestBody List<Integer> ids) {
        shoppingGuideTaskService.deleteBatch(ids);
        return Result.success();
    }

    @PostMapping("/execute/{id}")
    public Result execute(@PathVariable Integer id) {
        ShoppingGuideTask shoppingGuideTask = shoppingGuideTaskService.execute(id);
        return Result.success(shoppingGuideTask);
    }

    @GetMapping("/selectAll")
    public Result selectAll(ShoppingGuideTask shoppingGuideTask) {
        List<ShoppingGuideTask> list = shoppingGuideTaskService.selectAll(shoppingGuideTask);
        return Result.success(list);
    }

    @GetMapping("/selectPage")
    public Result selectPage(ShoppingGuideTask shoppingGuideTask,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<ShoppingGuideTask> pageInfo = shoppingGuideTaskService.selectPage(shoppingGuideTask, pageNum, pageSize);
        return Result.success(pageInfo);
    }
}
