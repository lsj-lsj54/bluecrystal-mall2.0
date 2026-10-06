package com.bluecrystal.api.client;

import com.bluecrystal.api.dto.OrderDTO;
import com.bluecrystal.common.domain.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

/**
 * 订单服务客户端（内部接口）。
 *
 * <p>路径统一挂在 {@code /internal/**} 下，网关**没有**为这个前缀配置路由，
 * 因此这些接口不对外暴露，只有服务间 Feign 调用能访问。
 */
@FeignClient(name = "trade-service", path = "/internal/orders", contextId = "tradeClient")
public interface TradeClient {

    /** 核对订单金额与状态；订单不属于当前登录用户时返回 403。 */
    @GetMapping("/{orderId}")
    R<OrderDTO> queryOrderById(@PathVariable("orderId") Long orderId);

    @PutMapping("/{orderId}/pay-success")
    R<Void> markOrderPaid(@PathVariable("orderId") Long orderId);
}
