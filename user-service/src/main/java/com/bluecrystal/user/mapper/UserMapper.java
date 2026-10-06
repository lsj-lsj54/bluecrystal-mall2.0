package com.bluecrystal.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecrystal.user.domain.po.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface UserMapper extends BaseMapper<User> {

    /**
     * 扣减余额：用 {@code balance >= #{amount}} 作为条件，保证不会扣成负数（乐观并发控制）。
     *
     * @return 受影响行数，0 表示余额不足
     */
    @Update("UPDATE `user` SET balance = balance - #{amount}, update_time = NOW() WHERE id = #{userId} AND balance >= #{amount}")
    int deductBalance(@Param("userId") Long userId, @Param("amount") Integer amount);
}
