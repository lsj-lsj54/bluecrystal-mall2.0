package com.bluecrystal.trade.controller;

import com.bluecrystal.api.dto.OrderDTO;
import com.bluecrystal.common.domain.R;
import com.bluecrystal.trade.service.IOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单服务的内部接口。
 *
 * <p>路径统一挂在 {@code /internal/**}：网关没有为该前缀配置路由，外部访问不到；
 * 这样「标记订单已支付」就只能由 pay-service 在支付流程里调用，用户无法跳过支付自己把订单改成已支付。
 */
@Tag(name = "订单内部接口")
@Validated
@RestController
@RequestMapping("/internal/orders")
@RequiredArgsConstructor
public class InternalOrderController {

    private final IOrderService orderService;

    @Operation(summary = "核对订单金额与状态（由 pay-service 调用）")
    @GetMapping("/{orderId}")
    public R<OrderDTO> queryForPayment(@PathVariable("orderId") Long orderId) {
        return R.ok(orderService.queryForPayment(orderId));
    }

    @Operation(summary = "支付成功回写订单状态（由 pay-service 调用）")
    @PutMapping("/{orderId}/pay-success")
    public R<Void> markPaid(@PathVariable("orderId") Long orderId) {
        orderService.markPaid(orderId);
        return R.ok();
    }
}
