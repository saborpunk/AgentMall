package com.example.controller;

import com.example.common.Result;
import com.example.entity.ProductKnowledge;
import com.example.service.ProductKnowledgeService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/productKnowledge")
public class ProductKnowledgeController {

    @Resource
    private ProductKnowledgeService productKnowledgeService;

    @PostMapping("/add")
    public Result add(@RequestBody ProductKnowledge productKnowledge) {
        productKnowledgeService.add(productKnowledge);
        return Result.success();
    }

    @PostMapping("/importFromProduct/{productId}")
    public Result importFromProduct(@PathVariable Integer productId) {
        int count = productKnowledgeService.importFromProduct(productId);
        return Result.success(count);
    }

    @PostMapping("/importAll")
    public Result importAll() {
        int count = productKnowledgeService.importAll();
        return Result.success(count);
    }

    @PutMapping("/update")
    public Result update(@RequestBody ProductKnowledge productKnowledge) {
        productKnowledgeService.updateById(productKnowledge);
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id) {
        productKnowledgeService.deleteById(id);
        return Result.success();
    }

    @DeleteMapping("/delete/batch")
    public Result delete(@RequestBody List<Integer> ids) {
        productKnowledgeService.deleteBatch(ids);
        return Result.success();
    }

    @GetMapping("/selectAll")
    public Result selectAll(ProductKnowledge productKnowledge) {
        List<ProductKnowledge> list = productKnowledgeService.selectAll(productKnowledge);
        return Result.success(list);
    }

    @GetMapping("/selectPage")
    public Result selectPage(ProductKnowledge productKnowledge,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<ProductKnowledge> pageInfo = productKnowledgeService.selectPage(productKnowledge, pageNum, pageSize);
        return Result.success(pageInfo);
    }
}
