package com.bluecrystal.trade.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单表 order。
 *
 * <p>order 是 MySQL 保留字，表名必须用反引号包裹，否则 SQL 无法解析。
 */
@Data
@TableName("`order`")
public class Order {

    /** 订单号：使用雪花 id，避免自增 id 暴露单量。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 订单总金额，单位：分。 */
    private Integer totalFee;

    /** 支付方式：1 支付宝，2 微信，3 余额。 */
    private Integer paymentType;

    private Long userId;

    /** 订单状态：1 待支付，2 已支付，3 已关闭。 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime payTime;

    private LocalDateTime consignTime;

    private LocalDateTime endTime;

    private LocalDateTime closeTime;

    private LocalDateTime updateTime;
}
