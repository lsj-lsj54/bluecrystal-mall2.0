package com.bluecrystal.cart.controller;

import com.bluecrystal.cart.domain.dto.CartFormDTO;
import com.bluecrystal.cart.domain.vo.CartVO;
import com.bluecrystal.cart.service.ICartService;
import com.bluecrystal.common.domain.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "购物车接口")
@Validated
@RestController
@RequestMapping("/carts")
@RequiredArgsConstructor
public class CartController {

    private final ICartService cartService;

    @Operation(summary = "查询当前用户购物车")
    @GetMapping
    public R<List<CartVO>> listOfCurrentUser() {
        return R.ok(cartService.listOfCurrentUser());
    }

    @Operation(summary = "加入购物车（同一商品已存在则累加数量）")
    @PostMapping
    public R<Void> addOrUpdate(@Valid @RequestBody CartFormDTO form) {
        cartService.addOrUpdate(form);
        return R.ok();
    }

    @Operation(summary = "批量移除购物车商品（由 trade-service 下单后调用）")
    @DeleteMapping
    public R<Void> removeByItemIds(@RequestParam("itemIds") List<Long> itemIds) {
        cartService.removeByItemIds(itemIds);
        return R.ok();
    }
}
