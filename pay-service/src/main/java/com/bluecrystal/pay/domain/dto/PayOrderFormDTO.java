package com.bluecrystal.pay.domain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 发起支付入参。
 *
 * @param bizOrderNo 业务订单号
 * @param amount 支付金额，单位：分（服务端会与订单真实金额比对）
 * @param payChannelCode 支付渠道编码，见 {@link com.bluecrystal.pay.enums.PayChannel}
 */
public record PayOrderFormDTO(
        @NotNull(message = "业务订单号不能为空") Long bizOrderNo,
        @NotNull(message = "支付金额不能为空") @Min(value = 1, message = "支付金额必须大于 0") Integer amount,
        @NotBlank(message = "支付渠道不能为空") String payChannelCode) {
}
