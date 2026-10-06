package com.bluecrystal.item.domain.dto;

/**
 * 扣减库存入参。
 *
 * @param itemId 商品 id
 * @param num 扣减数量
 */
public record ItemStockDeductDTO(Long itemId, Integer num) {
}
