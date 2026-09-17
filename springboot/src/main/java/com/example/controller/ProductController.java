package com.example.controller;

import com.example.common.Result;
import com.example.entity.Product;
import com.example.service.ProductService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/product")
public class ProductController {

    @Resource
    private ProductService productService;

    @PostMapping("/add")
    public Result add(@RequestBody Product product) {
        productService.add(product);
        return Result.success();
    }

    @PutMapping("/update")
    public Result update(@RequestBody Product product) {
        productService.updateById(product);
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id) {
        productService.deleteById(id);
        return Result.success();
    }

    @DeleteMapping("/delete/batch")
    public Result delete(@RequestBody List<Integer> ids) {
        productService.deleteBatch(ids);
        return Result.success();
    }

    @GetMapping("/selectAll")
    public Result selectAll(Product product) {
        List<Product> list = productService.selectAll(product);
        return Result.success(list);
    }

    @GetMapping("/selectPage")
    public Result selectPage(Product product,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<Product> pageInfo = productService.selectPage(product, pageNum, pageSize);
        return Result.success(pageInfo);
    }
}
