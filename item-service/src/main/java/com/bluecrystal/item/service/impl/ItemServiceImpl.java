package com.bluecrystal.item.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bluecrystal.common.domain.PageDTO;
import com.bluecrystal.common.exception.BizIllegalException;
import com.bluecrystal.common.exception.BadRequestException;
import com.bluecrystal.common.mybatis.PageConverter;
import com.bluecrystal.item.domain.dto.ItemStockDeductDTO;
import com.bluecrystal.item.domain.po.Item;
import com.bluecrystal.item.domain.query.ItemPageQuery;
import com.bluecrystal.item.domain.vo.ItemVO;
import com.bluecrystal.item.mapper.ItemMapper;
import com.bluecrystal.item.service.IItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements IItemService {

    private final ItemMapper itemMapper;

    @Override
    public ItemVO queryById(Long id) {
        Item item = itemMapper.selectById(id);
        if (item == null) {
            throw new BadRequestException("商品不存在：" + id);
        }
        return toVO(item);
    }

    @Override
    public List<ItemVO> queryByIds(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return List.of();
        }
        return itemMapper.selectByIds(ids).stream().map(this::toVO).toList();
    }

    @Override
    public PageDTO<ItemVO> pageQuery(ItemPageQuery query) {
        LambdaQueryWrapper<Item> wrapper = new LambdaQueryWrapper<Item>()
                .like(StringUtils.hasText(query.getName()), Item::getName, query.getName())
                .eq(StringUtils.hasText(query.getCategory()), Item::getCategory, query.getCategory())
                .eq(StringUtils.hasText(query.getBrand()), Item::getBrand, query.getBrand())
                .eq(Item::getStatus, 1)
                .orderByDesc(Item::getSold);
        Page<Item> page = itemMapper.selectPage(
                Page.of(query.getPageNo(), query.getPageSize()), wrapper);
        return PageConverter.toPageDTO(page, this::toVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deductStock(List<ItemStockDeductDTO> details) {
        if (CollectionUtils.isEmpty(details)) {
            throw new BadRequestException("扣减库存明细不能为空");
        }
        for (ItemStockDeductDTO detail : details) {
            int rows = itemMapper.deductStock(detail.itemId(), detail.num());
            if (rows == 0) {
                // 抛出异常让本地事务与全局事务一起回滚
                throw new BizIllegalException("商品库存不足：" + detail.itemId());
            }
        }
        log.info("扣减库存成功，共 {} 个商品", details.size());
    }

    private ItemVO toVO(Item item) {
        return new ItemVO(item.getId(), item.getName(), item.getPrice(), item.getImage(),
                item.getSpec(), item.getStock(), item.getSold());
    }
}
