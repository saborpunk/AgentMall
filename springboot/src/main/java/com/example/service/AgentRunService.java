package com.example.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.example.common.enums.ResultCodeEnum;
import com.example.entity.AgentRun;
import com.example.entity.ShoppingGuideTask;
import com.example.exception.CustomException;
import com.example.mapper.AgentRunMapper;
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
public class AgentRunService {

    @Resource
    private AgentRunMapper agentRunMapper;
    @Resource
    private AgentStepMapper agentStepMapper;

    public AgentRun startGuideRun(ShoppingGuideTask task) {
        String now = DateUtil.now();
        AgentRun agentRun = new AgentRun();
        agentRun.setRunNo("RUN" + DateUtil.format(DateUtil.date(), "yyyyMMddHHmmssSSS"));
        agentRun.setTaskId(task.getId());
        agentRun.setUserId(task.getUserId());
        agentRun.setRunType("SHOPPING_GUIDE");
        agentRun.setStatus("RUNNING");
        agentRun.setInputSnapshot(buildInputSnapshot(task));
        agentRun.setStartTime(now);
        agentRun.setCreateTime(now);
        agentRun.setUpdateTime(now);
        agentRunMapper.insert(agentRun);
        return agentRun;
    }

    public void finishGuideRun(AgentRun agentRun, ShoppingGuideTask task) {
        if (ObjectUtil.isNull(agentRun) || ObjectUtil.isEmpty(agentRun.getId())) {
            return;
        }
        String now = DateUtil.now();
        agentRun.setStatus(task.getStatus());
        agentRun.setOutputSummary(task.getRecommendationResult());
        agentRun.setErrorMessage("FAILED".equals(task.getStatus()) ? task.getExecuteMessage() : "");
        agentRun.setEndTime(now);
        agentRun.setDurationMs(calculateDuration(agentRun.getStartTime(), now));
        agentRun.setUpdateTime(now);
        agentRunMapper.updateById(agentRun);
    }

    // agent_run.error_message 是 varchar(500)，框架异常（如 MyBatis 的 SQL 异常）
    // 的 message 往往上千字，直接写库会触发 Data too long 报错
    private static final int MAX_ERROR_MESSAGE_LENGTH = 500;

    public void failGuideRun(AgentRun agentRun, String errorMessage) {
        if (ObjectUtil.isNull(agentRun) || ObjectUtil.isEmpty(agentRun.getId())) {
            return;
        }
        String now = DateUtil.now();
        agentRun.setStatus("FAILED");
        // 超长时只保留开头，开头正是异常的真正原因，后面通常是 SQL 语句和堆栈信息
        agentRun.setErrorMessage(StrUtil.maxLength(errorMessage, MAX_ERROR_MESSAGE_LENGTH));
        agentRun.setEndTime(now);
        agentRun.setDurationMs(calculateDuration(agentRun.getStartTime(), now));
        agentRun.setUpdateTime(now);
        agentRunMapper.updateById(agentRun);
    }

    public void deleteById(Integer id) {
        agentStepMapper.deleteByRunId(id);
        agentRunMapper.deleteById(id);
    }

    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            agentStepMapper.deleteByRunId(id);
            agentRunMapper.deleteById(id);
        }
    }

    public PageInfo<AgentRun> selectPage(AgentRun agentRun, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<AgentRun> list = agentRunMapper.selectAll(agentRun);
        return PageInfo.of(list);
    }

    private String buildInputSnapshot(ShoppingGuideTask task) {
        if (ObjectUtil.isNull(task)) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        StringBuilder builder = new StringBuilder();
        builder.append("任务编号：").append(task.getTaskNo()).append("\n");
        builder.append("用户ID：").append(task.getUserId()).append("\n");
        builder.append("预算金额：").append(task.getBudgetAmount()).append("\n");
        builder.append("基准商品ID：").append(task.getProductId()).append("\n");
        builder.append("购物需求：").append(task.getDemandText());
        return builder.toString();
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
