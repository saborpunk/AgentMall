package com.example.controller;

import com.example.common.Result;
import com.example.entity.ProductParam;
import com.example.service.ProductParamService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/productParam")
public class ProductParamController {

    @Resource
    private ProductParamService productParamService;

    @PostMapping("/add")
    public Result add(@RequestBody ProductParam productParam) {
        productParamService.add(productParam);
        return Result.success();
    }

    @PutMapping("/update")
    public Result update(@RequestBody ProductParam productParam) {
        productParamService.updateById(productParam);
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id) {
        productParamService.deleteById(id);
        return Result.success();
    }

    @DeleteMapping("/delete/batch")
    public Result delete(@RequestBody List<Integer> ids) {
        productParamService.deleteBatch(ids);
        return Result.success();
    }

    @GetMapping("/selectAll")
    public Result selectAll(ProductParam productParam) {
        List<ProductParam> list = productParamService.selectAll(productParam);
        return Result.success(list);
    }

}
