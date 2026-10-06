package com.bluecrystal.trade.controller;

import com.bluecrystal.common.domain.R;
import com.bluecrystal.trade.domain.dto.OrderFormDTO;
import com.bluecrystal.trade.domain.vo.OrderVO;
import com.bluecrystal.trade.service.IOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "订单接口")
@Validated
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final IOrderService orderService;

    @Operation(summary = "创建订单（Seata 全局事务入口）")
    @PostMapping
    public R<OrderVO> createOrder(@Valid @RequestBody OrderFormDTO form) {
        return R.ok(orderService.createOrder(form));
    }

    @Operation(summary = "查询订单详情")
    @GetMapping("/{orderId}")
    public R<OrderVO> queryById(@PathVariable("orderId") Long orderId) {
        return R.ok(orderService.queryById(orderId));
    }

    // 注意：「支付成功回写」已挪到 /internal/orders/**（InternalOrderController），
    // 不对外暴露，否则用户可以不付钱直接把自己的订单标记成已支付。
}
