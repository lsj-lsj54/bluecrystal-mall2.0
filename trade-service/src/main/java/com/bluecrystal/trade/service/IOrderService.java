package com.bluecrystal.trade.service;

import com.bluecrystal.trade.domain.dto.OrderFormDTO;
import com.bluecrystal.trade.domain.vo.OrderVO;

public interface IOrderService {

    /** 创建订单：扣库存 + 落库 + 清购物车，由 Seata 全局事务保证一致性。 */
    OrderVO createOrder(OrderFormDTO form);

    /** 查询订单详情，仅允许订单所属用户查看。 */
    OrderVO queryById(Long orderId);

    /** 支付成功回写订单状态，供 pay-service 通过 Feign 调用。 */
    void markPaid(Long orderId);
}
