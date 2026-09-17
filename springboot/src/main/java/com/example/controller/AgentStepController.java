package com.example.controller;

import com.example.common.Result;
import com.example.entity.AgentStep;
import com.example.service.AgentStepService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/agentStep")
public class AgentStepController {

    @Resource
    private AgentStepService agentStepService;

    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id) {
        agentStepService.deleteById(id);
        return Result.success();
    }

    @DeleteMapping("/delete/batch")
    public Result delete(@RequestBody List<Integer> ids) {
        agentStepService.deleteBatch(ids);
        return Result.success();
    }

    @GetMapping("/selectPage")
    public Result selectPage(AgentStep agentStep,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<AgentStep> pageInfo = agentStepService.selectPage(agentStep, pageNum, pageSize);
        return Result.success(pageInfo);
    }
}
