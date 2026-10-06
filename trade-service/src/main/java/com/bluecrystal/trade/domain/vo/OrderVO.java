package com.bluecrystal.trade.domain.vo;

import com.bluecrystal.api.dto.OrderDetailDTO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单视图对象。
 *
 * @param id 订单号
 * @param totalFee 订单总金额，单位：分
 * @param status 订单状态：1 待支付，2 已支付，3 已关闭
 * @param paymentType 支付方式
 * @param createTime 下单时间
 * @param details 订单明细
 */
public record OrderVO(Long id, Integer totalFee, Integer status, Integer paymentType,
                      LocalDateTime createTime, List<OrderDetailDTO> details) {
}
