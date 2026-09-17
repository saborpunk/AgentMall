package com.example.service;

import cn.hutool.core.date.DateUtil;
import com.example.entity.ShoppingRecommendation;
import com.example.mapper.ShoppingRecommendationMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ShoppingRecommendationService {

    @Resource
    private ShoppingRecommendationMapper shoppingRecommendationMapper;

    public void saveTaskRecommendations(Integer taskId, List<ShoppingRecommendation> recommendations) {
        shoppingRecommendationMapper.deleteByTaskId(taskId);
        if (recommendations == null || recommendations.isEmpty()) {
            return;
        }
        String now = DateUtil.now();
        for (ShoppingRecommendation recommendation : recommendations) {
            recommendation.setStatus("VALID");
            recommendation.setCreateTime(now);
            recommendation.setUpdateTime(now);
            shoppingRecommendationMapper.insert(recommendation);
        }
    }

    public void deleteById(Integer id) {
        shoppingRecommendationMapper.deleteById(id);
    }

    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            shoppingRecommendationMapper.deleteById(id);
        }
    }

    public List<ShoppingRecommendation> selectAll(ShoppingRecommendation shoppingRecommendation) {
        return shoppingRecommendationMapper.selectAll(shoppingRecommendation);
    }

    public PageInfo<ShoppingRecommendation> selectPage(ShoppingRecommendation shoppingRecommendation, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<ShoppingRecommendation> list = shoppingRecommendationMapper.selectAll(shoppingRecommendation);
        return PageInfo.of(list);
    }
}
