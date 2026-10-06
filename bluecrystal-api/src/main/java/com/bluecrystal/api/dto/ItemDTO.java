package com.bluecrystal.api.dto;

/**
 * 商品信息（跨服务传输）。
 *
 * @param id 商品 id
 * @param name 商品名称
 * @param price 价格，单位：分
 * @param image 主图地址
 * @param spec 规格描述（JSON 字符串）
 * @param stock 库存
 */
public record ItemDTO(Long id, String name, Integer price, String image, String spec, Integer stock) {
}
