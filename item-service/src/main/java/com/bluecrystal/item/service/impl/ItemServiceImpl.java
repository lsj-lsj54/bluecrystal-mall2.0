package com.bluecrystal.item.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bluecrystal.api.dto.ItemDTO;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements IItemService {

  @Autowired RedisTemplate<String, Object> redisTemplate;

  private final ItemMapper itemMapper;

  @Override
  public ItemDTO queryById(Long id) {
    // 1.先检查redis有没有
    Item item = (Item) redisTemplate.opsForValue().get(String.valueOf(id));
    // 2.1 如果没有，查数据库看有没有
    if (item == null) {
      item = itemMapper.selectById(id);
      // 2.1.1数据库也没有，redis存空值避免缓存穿透
      if (item == null) {
        redisTemplate.opsForValue().set(String.valueOf(id), new Item(), 60, TimeUnit.SECONDS);
        throw new BadRequestException("商品不存在：" + id);
      }
      // 2.1.2数据库有，写入redis
      redisTemplate.opsForValue().set(String.valueOf(id), item, 60, TimeUnit.MINUTES);
    }
    // 3.判断获得的item对象是不是空值，是的话抛异常
    if (item.getId() == null) {
      throw new BadRequestException("商品不存在：" + id);
    }
    // 4。转成跨服务 DTO 返回
    return toDTO(item);
  }

  @Override
  public List<ItemDTO> queryByIds(List<Long> ids) {
    // 1.先检查ids是否为空
    if (CollectionUtils.isEmpty(ids)) {
      return List.of();
    }
    // 2.先去reids找，找不到再去数据库找，数据库找到的话就存到redis
    List<ItemDTO> items = new ArrayList<>();
    for (long id : ids) {
      // 2.1在redis里找
      Item item = (Item) redisTemplate.opsForValue().get(String.valueOf(id));
      // 分支一 redis没有找到，去数据库找
      if (item == null) {
        item = itemMapper.selectById(id);
        // 分支一 数据库有，写入redis，存入列表
        if (item != null) {
          redisTemplate.opsForValue().set(String.valueOf(id), item, 60, TimeUnit.MINUTES);
          items.add(toDTO(item));
        }
        // 分支二 数据库也没有，写入空值到redis，防止缓存穿透
        else {
          redisTemplate.opsForValue().set(String.valueOf(id), new Item(), 60, TimeUnit.SECONDS);
        }
      }
      // 分支二 redis找到了，判断是不是空值,不是空值就存到列表，是空值不做任何操作
      else {
        if (item.getId() != null) {
          items.add(toDTO(item));
        }
      }
    }
    // 3.把DTO映射为VO返回前端

    return items;
  }

  @Override
  public PageDTO<ItemVO> pageQuery(ItemPageQuery query) {
    LambdaQueryWrapper<Item> wrapper =
        new LambdaQueryWrapper<Item>()
            .like(StringUtils.hasText(query.getName()), Item::getName, query.getName())
            .eq(StringUtils.hasText(query.getCategory()), Item::getCategory, query.getCategory())
            .eq(StringUtils.hasText(query.getBrand()), Item::getBrand, query.getBrand())
            .eq(Item::getStatus, 1)
            .orderByDesc(Item::getSold);
    Page<Item> page =
        itemMapper.selectPage(Page.of(query.getPageNo(), query.getPageSize()), wrapper);
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

  /** PO → 跨服务 DTO（字段与 ItemVO 对齐：id/name/price/image/spec/stock/sold）。 */
  private ItemDTO toDTO(Item item) {
    return new ItemDTO(
        item.getId(),
        item.getName(),
        item.getPrice(),
        item.getImage(),
        item.getSpec(),
        item.getStock(),
        item.getSold());
  }

  private ItemVO toVO(Item item) {
    return new ItemVO(
        item.getId(),
        item.getName(),
        item.getPrice(),
        item.getImage(),
        item.getSpec(),
        item.getStock(),
        item.getSold());
  }
}
