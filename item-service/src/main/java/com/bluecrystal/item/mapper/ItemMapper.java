package com.bluecrystal.item.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecrystal.item.domain.po.Item;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface ItemMapper extends BaseMapper<Item> {

    /**
     * 扣减库存：用 {@code stock >= num} 作为条件，保证不会扣成负数（乐观并发控制）。
     *
     * @return 受影响行数，0 表示库存不足
     */
    @Update("UPDATE item SET stock = stock - #{num}, sold = sold + #{num} WHERE id = #{id} AND stock >= #{num}")
    int deductStock(@Param("id") Long id, @Param("num") Integer num);
}
