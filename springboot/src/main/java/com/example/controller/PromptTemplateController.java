package com.example.controller;

import com.example.common.Result;
import com.example.entity.PromptTemplate;
import com.example.service.PromptTemplateService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/promptTemplate")
public class PromptTemplateController {

    @Resource
    private PromptTemplateService promptTemplateService;

    @PostMapping("/add")
    public Result add(@RequestBody PromptTemplate promptTemplate) {
        promptTemplateService.add(promptTemplate);
        return Result.success();
    }

    @PutMapping("/update")
    public Result update(@RequestBody PromptTemplate promptTemplate) {
        promptTemplateService.updateById(promptTemplate);
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id) {
        promptTemplateService.deleteById(id);
        return Result.success();
    }

    @DeleteMapping("/delete/batch")
    public Result delete(@RequestBody List<Integer> ids) {
        promptTemplateService.deleteBatch(ids);
        return Result.success();
    }

    @GetMapping("/selectPage")
    public Result selectPage(PromptTemplate promptTemplate,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<PromptTemplate> pageInfo = promptTemplateService.selectPage(promptTemplate, pageNum, pageSize);
        return Result.success(pageInfo);
    }
}
