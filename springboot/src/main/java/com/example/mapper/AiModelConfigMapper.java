package com.example.mapper;

import com.example.entity.AiModelConfig;
import java.util.List;

public interface AiModelConfigMapper {

    int insert(AiModelConfig aiModelConfig);

    void updateById(AiModelConfig aiModelConfig);

    void deleteById(Integer id);

    List<AiModelConfig> selectAll(AiModelConfig aiModelConfig);
}
