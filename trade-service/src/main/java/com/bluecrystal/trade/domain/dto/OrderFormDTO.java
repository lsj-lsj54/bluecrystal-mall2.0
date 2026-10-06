package com.bluecrystal.trade.domain.dto;

import com.bluecrystal.api.dto.OrderDetailDTO;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 下单入参。
 *
 * @param details 订单明细（商品 id、数量、下单快照信息）
 * @param paymentType 支付方式：1 支付宝，2 微信，3 余额
 */
public record OrderFormDTO(
        @NotEmpty(message = "订单明细不能为空") List<OrderDetailDTO> details,
        @NotNull(message = "支付方式不能为空") Integer paymentType) {
}
