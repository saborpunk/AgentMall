package com.example.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.example.common.enums.ResultCodeEnum;
import com.example.entity.FunctionTool;
import com.example.exception.CustomException;
import com.example.mapper.FunctionToolMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FunctionToolService {

    @Resource
    private FunctionToolMapper functionToolMapper;

    public void add(FunctionTool functionTool) {
        validate(functionTool);
        FunctionTool dbTool = functionToolMapper.selectByToolCode(functionTool.getToolCode());
        if (ObjectUtil.isNotNull(dbTool)) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        if (functionTool.getIsEnabled() == null) {
            functionTool.setIsEnabled(1);
        }
        if (functionTool.getSort() == null) {
            functionTool.setSort(1);
        }
        String now = DateUtil.now();
        functionTool.setCreateTime(now);
        functionTool.setUpdateTime(now);
        functionToolMapper.insert(functionTool);
    }

    public void updateById(FunctionTool functionTool) {
        if (ObjectUtil.isEmpty(functionTool.getId())) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        validate(functionTool);
        FunctionTool dbTool = functionToolMapper.selectByToolCode(functionTool.getToolCode());
        if (ObjectUtil.isNotNull(dbTool) && !dbTool.getId().equals(functionTool.getId())) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        if (functionTool.getSort() == null) {
            functionTool.setSort(1);
        }
        functionTool.setUpdateTime(DateUtil.now());
        functionToolMapper.updateById(functionTool);
    }

    public void deleteById(Integer id) {
        functionToolMapper.deleteById(id);
    }

    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            functionToolMapper.deleteById(id);
        }
    }

    public PageInfo<FunctionTool> selectPage(FunctionTool functionTool, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<FunctionTool> list = functionToolMapper.selectAll(functionTool);
        return PageInfo.of(list);
    }

    private void validate(FunctionTool functionTool) {
        if (ObjectUtil.isEmpty(functionTool.getToolCode())
                || ObjectUtil.isEmpty(functionTool.getToolName())
                || ObjectUtil.isEmpty(functionTool.getToolType())
                || ObjectUtil.isEmpty(functionTool.getInvokeType())
                || ObjectUtil.isEmpty(functionTool.getServiceBean())
                || ObjectUtil.isEmpty(functionTool.getServiceMethod())
                || ObjectUtil.isEmpty(functionTool.getInputSchema())
                || ObjectUtil.isEmpty(functionTool.getOutputSchema())) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
    }
}
