package com.bluecrystal.api.dto;

/**
 * 订单明细 / 扣减库存入参。
 *
 * @param itemId 商品 id
 * @param num 数量
 * @param name 商品名称（下单时快照）
 * @param price 单价，单位：分
 * @param image 图片地址
 * @param spec 规格
 */
public record OrderDetailDTO(Long itemId, Integer num, String name, Integer price, String image, String spec) {
}
