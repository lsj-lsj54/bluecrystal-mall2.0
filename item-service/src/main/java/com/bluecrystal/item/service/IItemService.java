package com.bluecrystal.item.service;

import com.bluecrystal.common.domain.PageDTO;
import com.bluecrystal.item.domain.dto.ItemStockDeductDTO;
import com.bluecrystal.item.domain.query.ItemPageQuery;
import com.bluecrystal.item.domain.vo.ItemVO;

import java.util.List;

public interface IItemService {

    ItemVO queryById(Long id);

    List<ItemVO> queryByIds(List<Long> ids);

    PageDTO<ItemVO> pageQuery(ItemPageQuery query);

    /** 批量扣减库存，任一条不足则整批回滚（配合 Seata 全局事务）。 */
    void deductStock(List<ItemStockDeductDTO> details);
}
