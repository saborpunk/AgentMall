package com.example.mapper;

import com.example.entity.AfterSaleRule;
import java.util.List;

public interface AfterSaleRuleMapper {

    int insert(AfterSaleRule afterSaleRule);

    void updateById(AfterSaleRule afterSaleRule);

    void deleteById(Integer id);

    List<AfterSaleRule> selectAll(AfterSaleRule afterSaleRule);
}
