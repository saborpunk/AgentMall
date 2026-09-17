package com.example.controller;

import com.example.common.Result;
import com.example.entity.ProductBrand;
import com.example.service.ProductBrandService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/productBrand")
public class ProductBrandController {

    @Resource
    private ProductBrandService productBrandService;

    @PostMapping("/add")
    public Result add(@RequestBody ProductBrand productBrand) {
        productBrandService.add(productBrand);
        return Result.success();
    }

    @PutMapping("/update")
    public Result update(@RequestBody ProductBrand productBrand) {
        productBrandService.updateById(productBrand);
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id) {
        productBrandService.deleteById(id);
        return Result.success();
    }

    @DeleteMapping("/delete/batch")
    public Result delete(@RequestBody List<Integer> ids) {
        productBrandService.deleteBatch(ids);
        return Result.success();
    }

    @GetMapping("/selectAll")
    public Result selectAll(ProductBrand productBrand) {
        List<ProductBrand> list = productBrandService.selectAll(productBrand);
        return Result.success(list);
    }

    @GetMapping("/selectPage")
    public Result selectPage(ProductBrand productBrand,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<ProductBrand> pageInfo = productBrandService.selectPage(productBrand, pageNum, pageSize);
        return Result.success(pageInfo);
    }
}
