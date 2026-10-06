package com.bluecrystal.cart.domain.vo;

/**
 * 购物车条目视图对象。
 *
 * <p>商品名称/规格/价格/图片来源于加购时的快照，缺失时由商品服务实时补全。
 *
 * @param id 购物车条目 id
 * @param itemId 商品 id
 * @param num 购买数量
 * @param name 商品名称
 * @param spec 规格描述
 * @param price 价格，单位：分
 * @param image 主图地址
 */
public record CartVO(Long id, Long itemId, Integer num, String name, String spec, Integer price, String image) {
}
