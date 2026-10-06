package com.bluecrystal.pay.controller;

import com.bluecrystal.common.domain.R;
import com.bluecrystal.pay.domain.dto.PayOrderFormDTO;
import com.bluecrystal.pay.domain.vo.PayOrderVO;
import com.bluecrystal.pay.service.IPayOrderService;
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

@Tag(name = "支付接口")
@Validated
@RestController
@RequestMapping("/pay-orders")
@RequiredArgsConstructor
public class PayController {

    private final IPayOrderService payOrderService;

    @Operation(summary = "发起支付（当前仅支持余额支付）")
    @PostMapping
    public R<PayOrderVO> applyPay(@Valid @RequestBody PayOrderFormDTO form) {
        return R.ok(payOrderService.applyPay(form));
    }

    @Operation(summary = "查询支付单详情")
    @GetMapping("/{id}")
    public R<PayOrderVO> queryById(@PathVariable("id") Long id) {
        return R.ok(payOrderService.queryById(id));
    }
}
