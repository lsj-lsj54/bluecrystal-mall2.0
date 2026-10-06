package com.bluecrystal.cart.domain.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 加入购物车入参。
 *
 * @param itemId 商品 id
 * @param num 购买数量，必须为正数（由业务层校验）
 */
public record CartFormDTO(
        @NotNull(message = "商品 id 不能为空") Long itemId,
        @NotNull(message = "购买数量不能为空") Integer num) {
}
