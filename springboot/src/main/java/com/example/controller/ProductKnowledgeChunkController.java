package com.example.controller;

import com.example.common.Result;
import com.example.entity.ProductKnowledgeChunk;
import com.example.service.ProductKnowledgeChunkService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/productKnowledgeChunk")
public class ProductKnowledgeChunkController {

    @Resource
    private ProductKnowledgeChunkService productKnowledgeChunkService;

    @PostMapping("/generate/{knowledgeId}")
    public Result generate(@PathVariable Integer knowledgeId) {
        int count = productKnowledgeChunkService.generateByKnowledgeId(knowledgeId);
        return Result.success(count);
    }

    @PostMapping("/generateAll")
    public Result generateAll() {
        int count = productKnowledgeChunkService.generateAll();
        return Result.success(count);
    }

    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id) {
        productKnowledgeChunkService.deleteById(id);
        return Result.success();
    }

    @DeleteMapping("/delete/batch")
    public Result delete(@RequestBody List<Integer> ids) {
        productKnowledgeChunkService.deleteBatch(ids);
        return Result.success();
    }

    @GetMapping("/selectAll")
    public Result selectAll(ProductKnowledgeChunk productKnowledgeChunk) {
        List<ProductKnowledgeChunk> list = productKnowledgeChunkService.selectAll(productKnowledgeChunk);
        return Result.success(list);
    }

    @GetMapping("/selectPage")
    public Result selectPage(ProductKnowledgeChunk productKnowledgeChunk,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<ProductKnowledgeChunk> pageInfo = productKnowledgeChunkService.selectPage(productKnowledgeChunk, pageNum, pageSize);
        return Result.success(pageInfo);
    }
}
