package com.bluecrystal.pay.domain.vo;

import java.time.LocalDateTime;

/**
 * 支付单视图对象。
 *
 * @param id 支付单 id
 * @param bizOrderNo 业务订单号
 * @param amount 支付金额，单位：分
 * @param status 支付单状态：0 待支付，1 已支付，2 已关闭
 * @param payChannelCode 支付渠道编码
 * @param paySuccessTime 支付成功时间
 */
public record PayOrderVO(Long id, Long bizOrderNo, Integer amount, Integer status,
                        String payChannelCode, LocalDateTime paySuccessTime) {
}
