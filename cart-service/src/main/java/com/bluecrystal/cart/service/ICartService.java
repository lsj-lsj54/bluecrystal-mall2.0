package com.bluecrystal.cart.service;

import com.bluecrystal.cart.domain.dto.CartFormDTO;
import com.bluecrystal.cart.domain.vo.CartVO;

import java.util.List;

public interface ICartService {

    /** 查询当前登录用户的购物车，商品信息缺失时由 item-service 补全。 */
    List<CartVO> listOfCurrentUser();

    /** 加入购物车：同一用户同一商品已存在则累加数量，否则新增一条。 */
    void addOrUpdate(CartFormDTO form);

    /** 按当前用户批量移除购物车条目（下单后由 trade-service 调用）。 */
    void removeByItemIds(List<Long> itemIds);
}
