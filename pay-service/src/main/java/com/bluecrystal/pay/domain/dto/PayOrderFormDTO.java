package com.bluecrystal.pay.domain.dto;

/**
 * 发起支付入参。
 *
 * @param bizOrderNo 业务订单号
 * @param amount 支付金额，单位：分
 * @param payChannelCode 支付渠道编码，见 {@link com.bluecrystal.pay.enums.PayChannel}
 */
public record PayOrderFormDTO(Long bizOrderNo, Integer amount, String payChannelCode) {
}
