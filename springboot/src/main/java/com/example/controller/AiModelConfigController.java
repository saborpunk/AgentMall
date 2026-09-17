package com.example.controller;

import com.example.common.Result;
import com.example.entity.AiModelConfig;
import com.example.service.AiModelConfigService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/aiModelConfig")
public class AiModelConfigController {

    @Resource
    private AiModelConfigService aiModelConfigService;

    @PostMapping("/add")
    public Result add(@RequestBody AiModelConfig aiModelConfig) {
        aiModelConfigService.add(aiModelConfig);
        return Result.success();
    }

    @PutMapping("/update")
    public Result update(@RequestBody AiModelConfig aiModelConfig) {
        aiModelConfigService.updateById(aiModelConfig);
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id) {
        aiModelConfigService.deleteById(id);
        return Result.success();
    }

    @DeleteMapping("/delete/batch")
    public Result delete(@RequestBody List<Integer> ids) {
        aiModelConfigService.deleteBatch(ids);
        return Result.success();
    }

    @GetMapping("/selectPage")
    public Result selectPage(AiModelConfig aiModelConfig,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<AiModelConfig> pageInfo = aiModelConfigService.selectPage(aiModelConfig, pageNum, pageSize);
        return Result.success(pageInfo);
    }
}
