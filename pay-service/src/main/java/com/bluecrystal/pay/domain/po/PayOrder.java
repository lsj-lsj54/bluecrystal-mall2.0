package com.bluecrystal.pay.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 支付单表 pay_order。 */
@Data
@TableName("pay_order")
public class PayOrder {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 业务订单号（trade-service 的订单 id）。 */
    private Long bizOrderNo;

    /** 支付单号，支付成功后生成。 */
    private Long payOrderNo;

    /** 支付用户 id。 */
    private Long bizUserId;

    /** 支付渠道编码，见 {@link com.bluecrystal.pay.enums.PayChannel}。 */
    private String payChannelCode;

    /** 支付金额，单位：分。 */
    private Integer amount;

    /** 支付方式，为后续接入第三方渠道预留。 */
    private Integer payType;

    /** 支付单状态：0 待支付，1 已支付，2 已关闭。 */
    private Integer status;

    /** 扩展参数，JSON 字符串。 */
    private String expandJson;

    /** 渠道返回码。 */
    private String resultCode;

    /** 渠道返回信息。 */
    private String resultMsg;

    /** 支付成功时间。 */
    private LocalDateTime paySuccessTime;

    /** 支付超时时间。 */
    private LocalDateTime payOverTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
