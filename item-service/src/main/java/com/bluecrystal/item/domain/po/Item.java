package com.bluecrystal.item.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 商品表 item。 */
@Data
@TableName("item")
public class Item {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /** 价格，单位：分。 */
    private Integer price;

    private Integer stock;

    private String image;

    private String category;

    private String brand;

    /** 规格描述，JSON 字符串。 */
    private String spec;

    private Integer sold;

    private Integer commentCount;

    /** 是否为广告位：0 否，1 是。 */
    private Integer isAd;

    /** 1 上架，2 下架，3 已删除。 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
