package com.bluecrystal.item.service;

import com.bluecrystal.api.dto.ItemDTO;
import com.bluecrystal.common.domain.PageDTO;
import com.bluecrystal.item.domain.dto.ItemStockDeductDTO;
import com.bluecrystal.item.domain.query.ItemPageQuery;
import com.bluecrystal.item.domain.vo.ItemVO;

import java.util.List;

public interface IItemService {

    /** 按 id 查询商品，返回跨服务 DTO（供 controller 直接透出给 Feign 调用方）。 */
    ItemDTO queryById(Long id);

    /** 按 id 批量查询商品，返回跨服务 DTO 列表。 */
    List<ItemDTO> queryByIds(List<Long> ids);

    PageDTO<ItemVO> pageQuery(ItemPageQuery query);

    /** 批量扣减库存，任一条不足则整批回滚（配合 Seata 全局事务）。 */
    void deductStock(List<ItemStockDeductDTO> details);
}
