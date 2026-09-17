package com.example.mapper;

import com.example.entity.WalletRecord;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface WalletRecordMapper {

    int insert(WalletRecord walletRecord);

    @Select("""
            select wr.*, u.name as userName
            from wallet_record wr
            left join `user` u on wr.user_id = u.id
            where wr.user_id = #{userId}
            order by wr.id desc
            """)
    List<WalletRecord> selectByUserId(Integer userId);
}
