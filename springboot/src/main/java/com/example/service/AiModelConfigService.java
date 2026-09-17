package com.example.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.example.common.enums.ResultCodeEnum;
import com.example.entity.AiModelConfig;
import com.example.exception.CustomException;
import com.example.mapper.AiModelConfigMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AiModelConfigService {

    @Resource
    private AiModelConfigMapper aiModelConfigMapper;

    public void add(AiModelConfig aiModelConfig) {
        validate(aiModelConfig);
        if (aiModelConfig.getTemperature() == null) {
            aiModelConfig.setTemperature(new BigDecimal("0.70"));
        }
        if (aiModelConfig.getMaxTokens() == null) {
            aiModelConfig.setMaxTokens(2048);
        }
        if (aiModelConfig.getIsEnabled() == null) {
            aiModelConfig.setIsEnabled(1);
        }
        String now = DateUtil.now();
        aiModelConfig.setCreateTime(now);
        aiModelConfig.setUpdateTime(now);
        aiModelConfigMapper.insert(aiModelConfig);
    }

    public void updateById(AiModelConfig aiModelConfig) {
        if (ObjectUtil.isEmpty(aiModelConfig.getId())) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        validate(aiModelConfig);
        aiModelConfig.setUpdateTime(DateUtil.now());
        aiModelConfigMapper.updateById(aiModelConfig);
    }

    public void deleteById(Integer id) {
        aiModelConfigMapper.deleteById(id);
    }

    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            aiModelConfigMapper.deleteById(id);
        }
    }

    public PageInfo<AiModelConfig> selectPage(AiModelConfig aiModelConfig, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<AiModelConfig> list = aiModelConfigMapper.selectAll(aiModelConfig);
        return PageInfo.of(list);
    }

    private void validate(AiModelConfig aiModelConfig) {
        if (ObjectUtil.isEmpty(aiModelConfig.getProvider())
                || ObjectUtil.isEmpty(aiModelConfig.getModelName())
                || ObjectUtil.isEmpty(aiModelConfig.getBaseUrl())
                || ObjectUtil.isEmpty(aiModelConfig.getApiKey())) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
    }
}
