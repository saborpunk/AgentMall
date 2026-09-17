package com.example.mapper;

import com.example.entity.UserAddress;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface UserAddressMapper {

    int insert(UserAddress userAddress);

    void updateById(UserAddress userAddress);

    void deleteById(Integer id);

    @Select("select * from user_address where user_id = #{userId} order by is_default desc, id desc")
    List<UserAddress> selectByUserId(Integer userId);

    @Update("update user_address set is_default = 0, update_time = now() where user_id = #{userId}")
    void clearDefault(Integer userId);

    @Update("update user_address set is_default = 1, update_time = now() where id = #{id} and user_id = #{userId}")
    void setDefault(@Param("id") Integer id, @Param("userId") Integer userId);
}
