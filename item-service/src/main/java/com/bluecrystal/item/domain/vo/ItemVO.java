package com.bluecrystal.item.domain.vo;

/**
 * 商品视图对象。
 *
 * <p>字段与 {@code com.bluecrystal.api.dto.ItemDTO} 保持一致，便于 Feign 调用方直接反序列化。
 */
public record ItemVO(Long id, String name, Integer price, String image, String spec, Integer stock, Integer sold) {
}
