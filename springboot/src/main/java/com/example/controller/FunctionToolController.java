package com.example.controller;

import com.example.common.Result;
import com.example.entity.FunctionTool;
import com.example.service.FunctionToolService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/functionTool")
public class FunctionToolController {

    @Resource
    private FunctionToolService functionToolService;

    @PostMapping("/add")
    public Result add(@RequestBody FunctionTool functionTool) {
        functionToolService.add(functionTool);
        return Result.success();
    }

    @PutMapping("/update")
    public Result update(@RequestBody FunctionTool functionTool) {
        functionToolService.updateById(functionTool);
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id) {
        functionToolService.deleteById(id);
        return Result.success();
    }

    @DeleteMapping("/delete/batch")
    public Result delete(@RequestBody List<Integer> ids) {
        functionToolService.deleteBatch(ids);
        return Result.success();
    }

    @GetMapping("/selectPage")
    public Result selectPage(FunctionTool functionTool,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<FunctionTool> pageInfo = functionToolService.selectPage(functionTool, pageNum, pageSize);
        return Result.success(pageInfo);
    }
}
