package com.example.mapper;

import com.example.entity.ShoppingGuideTask;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ShoppingGuideTaskMapper {

    int insert(ShoppingGuideTask shoppingGuideTask);

    void updateById(ShoppingGuideTask shoppingGuideTask);

    void deleteById(Integer id);

    @Select("select * from shopping_guide_task where id = #{id}")
    ShoppingGuideTask selectById(Integer id);

    @Select("select * from shopping_guide_task where task_no = #{taskNo}")
    ShoppingGuideTask selectByTaskNo(String taskNo);

    List<ShoppingGuideTask> selectAll(ShoppingGuideTask shoppingGuideTask);
}
