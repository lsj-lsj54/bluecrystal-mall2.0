package com.bluecrystal.api.client;

import com.bluecrystal.common.domain.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

/**
 * 订单服务客户端：支付成功后回写订单状态。
 */
@FeignClient(name = "trade-service", path = "/orders", contextId = "tradeClient")
public interface TradeClient {

    @PutMapping("/{orderId}/pay-success")
    R<Void> markOrderPaid(@PathVariable("orderId") Long orderId);
}
