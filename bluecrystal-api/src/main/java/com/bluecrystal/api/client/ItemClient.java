package com.bluecrystal.api.client;

import com.bluecrystal.api.dto.ItemDTO;
import com.bluecrystal.api.dto.OrderDetailDTO;
import com.bluecrystal.common.domain.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 商品服务客户端。服务名 item-service 由 Nacos 服务发现解析。
 */
@FeignClient(name = "item-service", path = "/items", contextId = "itemClient")
public interface ItemClient {

    @GetMapping("/{id}")
    R<ItemDTO> queryItemById(@PathVariable("id") Long id);

    /** 批量查询商品，供购物车补全信息使用。 */
    @GetMapping
    R<List<ItemDTO>> queryItemsByIds(@RequestParam("ids") List<Long> ids);

    /** 下单扣减库存，由 trade-service 在全局事务中调用。 */
    @PostMapping("/stock/deduct")
    R<Void> deductStock(@RequestBody List<OrderDetailDTO> details);
}
