package com.bluecrystal.cart.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 购物车表 cart。 */
@Data
@TableName("cart")
public class Cart {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属用户 id。 */
    private Long userId;

    private Long itemId;

    /** 购买数量。 */
    private Integer num;

    /** 商品名称快照，可能为空，查询时由商品服务补全。 */
    private String name;

    /** 规格描述快照，JSON 字符串，可能为空。 */
    private String spec;

    /** 加购时价格，单位：分，可能为空。 */
    private Integer price;

    /** 主图地址快照，可能为空。 */
    private String image;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
