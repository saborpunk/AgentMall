package com.example.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.example.common.enums.ResultCodeEnum;
import com.example.entity.PromptTemplate;
import com.example.exception.CustomException;
import com.example.mapper.PromptTemplateMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PromptTemplateService {

    @Resource
    private PromptTemplateMapper promptTemplateMapper;

    public void add(PromptTemplate promptTemplate) {
        validate(promptTemplate);
        PromptTemplate dbTemplate = promptTemplateMapper.selectByTemplateCode(promptTemplate.getTemplateCode());
        if (ObjectUtil.isNotNull(dbTemplate)) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        if (promptTemplate.getIsEnabled() == null) {
            promptTemplate.setIsEnabled(1);
        }
        String now = DateUtil.now();
        promptTemplate.setCreateTime(now);
        promptTemplate.setUpdateTime(now);
        promptTemplateMapper.insert(promptTemplate);
    }

    public void updateById(PromptTemplate promptTemplate) {
        if (ObjectUtil.isEmpty(promptTemplate.getId())) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        validate(promptTemplate);
        PromptTemplate dbTemplate = promptTemplateMapper.selectByTemplateCode(promptTemplate.getTemplateCode());
        if (ObjectUtil.isNotNull(dbTemplate) && !dbTemplate.getId().equals(promptTemplate.getId())) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        promptTemplate.setUpdateTime(DateUtil.now());
        promptTemplateMapper.updateById(promptTemplate);
    }

    public void deleteById(Integer id) {
        promptTemplateMapper.deleteById(id);
    }

    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            promptTemplateMapper.deleteById(id);
        }
    }

    public PageInfo<PromptTemplate> selectPage(PromptTemplate promptTemplate, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<PromptTemplate> list = promptTemplateMapper.selectAll(promptTemplate);
        return PageInfo.of(list);
    }

    private void validate(PromptTemplate promptTemplate) {
        if (ObjectUtil.isEmpty(promptTemplate.getTemplateCode())
                || ObjectUtil.isEmpty(promptTemplate.getTemplateName())
                || ObjectUtil.isEmpty(promptTemplate.getBusinessType())
                || ObjectUtil.isEmpty(promptTemplate.getSystemPrompt())
                || ObjectUtil.isEmpty(promptTemplate.getUserPrompt())) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
    }
}
