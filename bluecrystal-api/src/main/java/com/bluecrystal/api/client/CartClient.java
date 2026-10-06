package com.bluecrystal.api.client;

import com.bluecrystal.common.domain.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 购物车服务客户端：下单成功后清理已下单的商品。
 */
@FeignClient(name = "cart-service", path = "/carts", contextId = "cartClient")
public interface CartClient {

    @DeleteMapping
    R<Void> removeByItemIds(@RequestParam("itemIds") List<Long> itemIds);
}
