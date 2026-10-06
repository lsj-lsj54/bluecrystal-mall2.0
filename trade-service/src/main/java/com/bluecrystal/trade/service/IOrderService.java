package com.bluecrystal.trade.service;

import com.bluecrystal.api.dto.OrderDTO;
import com.bluecrystal.trade.domain.dto.OrderFormDTO;
import com.bluecrystal.trade.domain.vo.OrderVO;

public interface IOrderService {

    /** 创建订单：回查商品价格 + 扣库存 + 落库 + 清购物车，由 Seata 全局事务保证一致性。 */
    OrderVO createOrder(OrderFormDTO form);

    /** 查询订单详情，仅允许订单所属用户查看。 */
    OrderVO queryById(Long orderId);

    /** 供 pay-service 核对订单金额与状态（内部接口），仅允许订单所属用户调用。 */
    OrderDTO queryForPayment(Long orderId);

    /** 支付成功回写订单状态，供 pay-service 通过 Feign 调用（内部接口）。 */
    void markPaid(Long orderId);
}
