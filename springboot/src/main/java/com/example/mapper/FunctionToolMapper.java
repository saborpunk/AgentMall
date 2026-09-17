package com.example.mapper;

import com.example.entity.FunctionTool;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface FunctionToolMapper {

    int insert(FunctionTool functionTool);

    void updateById(FunctionTool functionTool);

    void deleteById(Integer id);

    @Select("select * from function_tool where tool_code = #{toolCode}")
    FunctionTool selectByToolCode(String toolCode);

    List<FunctionTool> selectAll(FunctionTool functionTool);
}
