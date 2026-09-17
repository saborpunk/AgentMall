package com.example.controller;

import com.example.common.Result;
import com.example.entity.EmbeddingSearchRequest;
import com.example.entity.ProductKnowledgeEmbedding;
import com.example.service.ProductKnowledgeEmbeddingService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/productKnowledgeEmbedding")
public class ProductKnowledgeEmbeddingController {

    @Resource
    private ProductKnowledgeEmbeddingService productKnowledgeEmbeddingService;

    @PostMapping("/generate/{chunkId}")
    public Result generate(@PathVariable Integer chunkId) {
        int count = productKnowledgeEmbeddingService.generateByChunkId(chunkId);
        return Result.success(count);
    }

    @PostMapping("/generateAll")
    public Result generateAll() {
        // 只负责启动后台任务并返回待处理总数，真正的生成过程由前端轮询进度接口跟踪
        int total = productKnowledgeEmbeddingService.startGenerateAll();
        return Result.success(total);
    }

    @GetMapping("/generateProgress")
    public Result generateProgress() {
        return Result.success(productKnowledgeEmbeddingService.getGenerateProgress());
    }

    @PostMapping("/search")
    public Result search(@RequestBody EmbeddingSearchRequest request) {
        List<ProductKnowledgeEmbedding> list = productKnowledgeEmbeddingService.search(request);
        return Result.success(list);
    }

    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id) {
        productKnowledgeEmbeddingService.deleteById(id);
        return Result.success();
    }

    @DeleteMapping("/delete/batch")
    public Result delete(@RequestBody List<Integer> ids) {
        productKnowledgeEmbeddingService.deleteBatch(ids);
        return Result.success();
    }

    @GetMapping("/selectPage")
    public Result selectPage(ProductKnowledgeEmbedding productKnowledgeEmbedding,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<ProductKnowledgeEmbedding> pageInfo = productKnowledgeEmbeddingService.selectPage(productKnowledgeEmbedding, pageNum, pageSize);
        return Result.success(pageInfo);
    }
}
