package com.bluecrystal.trade.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单明细表 order_detail。
 *
 * <p>商品信息在下单时快照落库，后续商品改价不影响历史订单。
 */
@Data
@TableName("order_detail")
public class OrderDetail {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属订单号。 */
    private Long orderId;

    private Long itemId;

    /** 购买数量。 */
    private Integer num;

    /** 商品名称（下单快照）。 */
    private String name;

    /** 规格（下单快照），JSON 字符串。 */
    private String spec;

    /** 单价，单位：分（下单快照）。 */
    private Integer price;

    /** 图片地址（下单快照）。 */
    private String image;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
