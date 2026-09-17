package com.example.controller;

import com.example.common.Result;
import com.example.entity.ProductCategory;
import com.example.service.ProductCategoryService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/productCategory")
public class ProductCategoryController {

    @Resource
    private ProductCategoryService productCategoryService;

    @PostMapping("/add")
    public Result add(@RequestBody ProductCategory productCategory) {
        productCategoryService.add(productCategory);
        return Result.success();
    }

    @PutMapping("/update")
    public Result update(@RequestBody ProductCategory productCategory) {
        productCategoryService.updateById(productCategory);
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id) {
        productCategoryService.deleteById(id);
        return Result.success();
    }

    @DeleteMapping("/delete/batch")
    public Result delete(@RequestBody List<Integer> ids) {
        productCategoryService.deleteBatch(ids);
        return Result.success();
    }

    @GetMapping("/selectAll")
    public Result selectAll(ProductCategory productCategory) {
        List<ProductCategory> list = productCategoryService.selectAll(productCategory);
        return Result.success(list);
    }

    @GetMapping("/selectPage")
    public Result selectPage(ProductCategory productCategory,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<ProductCategory> pageInfo = productCategoryService.selectPage(productCategory, pageNum, pageSize);
        return Result.success(pageInfo);
    }
}
