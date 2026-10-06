package com.bluecrystal.api.dto;

/**
 * 订单信息（跨服务传输，供支付服务核对金额与状态）。
 *
 * @param id 订单号
 * @param userId 下单用户
 * @param totalFee 订单总金额，单位：分
 * @param status 订单状态：1 待支付，2 已支付，3 已关闭
 */
public record OrderDTO(Long id, Long userId, Integer totalFee, Integer status) {
}
