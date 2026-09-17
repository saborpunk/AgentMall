package com.example.controller;

import com.example.common.Result;
import com.example.entity.AgentRun;
import com.example.service.AgentRunService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/agentRun")
public class AgentRunController {

    @Resource
    private AgentRunService agentRunService;

    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id) {
        agentRunService.deleteById(id);
        return Result.success();
    }

    @DeleteMapping("/delete/batch")
    public Result delete(@RequestBody List<Integer> ids) {
        agentRunService.deleteBatch(ids);
        return Result.success();
    }

    @GetMapping("/selectPage")
    public Result selectPage(AgentRun agentRun,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<AgentRun> pageInfo = agentRunService.selectPage(agentRun, pageNum, pageSize);
        return Result.success(pageInfo);
    }
}
