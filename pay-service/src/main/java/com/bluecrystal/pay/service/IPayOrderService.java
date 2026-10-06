package com.bluecrystal.pay.service;

import com.bluecrystal.pay.domain.dto.PayOrderFormDTO;
import com.bluecrystal.pay.domain.vo.PayOrderVO;

public interface IPayOrderService {

    /**
     * 发起支付：骨架只实现余额支付链路。
     *
     * <p>扣余额（user-service）与回写订单状态（trade-service）在同一全局事务中，
     * 任一步失败都整体回滚。
     */
    PayOrderVO applyPay(PayOrderFormDTO form);

    /** 查询支付单详情，只允许支付单本人查看。 */
    PayOrderVO queryById(Long id);
}
