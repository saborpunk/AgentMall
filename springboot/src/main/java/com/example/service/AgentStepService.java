package com.example.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.example.entity.AgentRun;
import com.example.entity.AgentStep;
import com.example.mapper.AgentStepMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class AgentStepService {

    @Resource
    private AgentStepMapper agentStepMapper;

    public AgentStep startStep(AgentRun agentRun,
                               Integer stepOrder,
                               String stepCode,
                               String stepName,
                               String toolCode,
                               String inputContent) {
        if (ObjectUtil.isNull(agentRun) || ObjectUtil.isEmpty(agentRun.getId())) {
            return null;
        }
        String now = DateUtil.now();
        AgentStep step = new AgentStep();
        step.setRunId(agentRun.getId());
        step.setStepOrder(stepOrder);
        step.setStepCode(stepCode);
        step.setStepName(stepName);
        step.setToolCode(toolCode);
        step.setStatus("RUNNING");
        step.setInputContent(inputContent);
        step.setStartTime(now);
        step.setCreateTime(now);
        step.setUpdateTime(now);
        agentStepMapper.insert(step);
        return step;
    }

    public void finishStep(AgentStep step, String outputContent) {
        if (ObjectUtil.isNull(step) || ObjectUtil.isEmpty(step.getId())) {
            return;
        }
        String now = DateUtil.now();
        step.setStatus("DONE");
        step.setOutputContent(outputContent);
        step.setEndTime(now);
        step.setDurationMs(calculateDuration(step.getStartTime(), now));
        step.setUpdateTime(now);
        agentStepMapper.updateById(step);
    }

    public void failStep(AgentStep step, String errorMessage) {
        if (ObjectUtil.isNull(step) || ObjectUtil.isEmpty(step.getId())) {
            return;
        }
        String now = DateUtil.now();
        step.setStatus("FAILED");
        step.setErrorMessage(errorMessage);
        step.setEndTime(now);
        step.setDurationMs(calculateDuration(step.getStartTime(), now));
        step.setUpdateTime(now);
        agentStepMapper.updateById(step);
    }

    public void deleteById(Integer id) {
        agentStepMapper.deleteById(id);
    }

    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            agentStepMapper.deleteById(id);
        }
    }

    public PageInfo<AgentStep> selectPage(AgentStep agentStep, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<AgentStep> list = agentStepMapper.selectAll(agentStep);
        return PageInfo.of(list);
    }

    private Long calculateDuration(String startTime, String endTime) {
        if (ObjectUtil.isEmpty(startTime) || ObjectUtil.isEmpty(endTime)) {
            return 0L;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime start = LocalDateTime.parse(startTime, formatter);
        LocalDateTime end = LocalDateTime.parse(endTime, formatter);
        return Duration.between(start, end).toMillis();
    }
}
