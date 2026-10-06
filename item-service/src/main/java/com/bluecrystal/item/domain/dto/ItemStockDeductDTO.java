package com.bluecrystal.item.domain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 扣减库存入参。
 *
 * <p>必须校验 {@code num >= 1}：扣减 SQL 用的是 {@code stock >= #{num}} 条件更新，
 * 如果传入负数会变成「加库存」，属于数据正确性问题。
 *
 * @param itemId 商品 id
 * @param num 扣减数量，必须大于 0
 */
public record ItemStockDeductDTO(
        @NotNull(message = "商品 id 不能为空") Long itemId,
        @NotNull(message = "扣减数量不能为空") @Min(value = 1, message = "扣减数量必须大于 0") Integer num) {
}
