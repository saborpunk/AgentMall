package com.example.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.example.common.enums.ResultCodeEnum;
import com.example.entity.AfterSaleRule;
import com.example.exception.CustomException;
import com.example.mapper.AfterSaleRuleMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AfterSaleRuleService {

    @Resource
    private AfterSaleRuleMapper afterSaleRuleMapper;

    public void add(AfterSaleRule afterSaleRule) {
        validate(afterSaleRule);
        if (afterSaleRule.getIsEnabled() == null) {
            afterSaleRule.setIsEnabled(1);
        }
        if (afterSaleRule.getSort() == null) {
            afterSaleRule.setSort(1);
        }
        String now = DateUtil.now();
        afterSaleRule.setCreateTime(now);
        afterSaleRule.setUpdateTime(now);
        afterSaleRuleMapper.insert(afterSaleRule);
    }

    public void updateById(AfterSaleRule afterSaleRule) {
        if (ObjectUtil.isEmpty(afterSaleRule.getId())) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        validate(afterSaleRule);
        afterSaleRule.setUpdateTime(DateUtil.now());
        afterSaleRuleMapper.updateById(afterSaleRule);
    }

    public void deleteById(Integer id) {
        afterSaleRuleMapper.deleteById(id);
    }

    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            afterSaleRuleMapper.deleteById(id);
        }
    }

    public List<AfterSaleRule> selectAll(AfterSaleRule afterSaleRule) {
        return afterSaleRuleMapper.selectAll(afterSaleRule);
    }

    public PageInfo<AfterSaleRule> selectPage(AfterSaleRule afterSaleRule, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<AfterSaleRule> list = afterSaleRuleMapper.selectAll(afterSaleRule);
        return PageInfo.of(list);
    }

    private void validate(AfterSaleRule afterSaleRule) {
        if (ObjectUtil.isEmpty(afterSaleRule.getRuleName())
                || ObjectUtil.isEmpty(afterSaleRule.getRuleType())
                || ObjectUtil.isEmpty(afterSaleRule.getApplyScene())
                || ObjectUtil.isEmpty(afterSaleRule.getConditionText())
                || ObjectUtil.isEmpty(afterSaleRule.getProcessText())) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
    }
}
