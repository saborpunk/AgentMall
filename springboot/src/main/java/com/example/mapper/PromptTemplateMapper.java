package com.example.mapper;

import com.example.entity.PromptTemplate;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface PromptTemplateMapper {

    int insert(PromptTemplate promptTemplate);

    void updateById(PromptTemplate promptTemplate);

    void deleteById(Integer id);

    @Select("select * from prompt_template where template_code = #{templateCode}")
    PromptTemplate selectByTemplateCode(String templateCode);

    List<PromptTemplate> selectAll(PromptTemplate promptTemplate);
}
