package com.bluecrystal.item.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bluecrystal.api.dto.ItemDTO;
import com.bluecrystal.common.domain.PageDTO;
import com.bluecrystal.common.exception.BadRequestException;
import com.bluecrystal.common.exception.BizIllegalException;
import com.bluecrystal.common.mybatis.PageConverter;
import com.bluecrystal.item.domain.dto.ItemStockDeductDTO;
import com.bluecrystal.item.domain.po.Item;
import com.bluecrystal.item.domain.query.ItemPageQuery;
import com.bluecrystal.item.domain.vo.ItemVO;
import com.bluecrystal.item.mapper.ItemMapper;
import com.bluecrystal.item.service.IItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements IItemService {

    /** 正常商品的缓存时长。 */
    private static final Duration ITEM_CACHE_TTL = Duration.ofMinutes(60);

    /** 空值占位的缓存时长：防止不存在的 id 每次都打库（缓存穿透）。 */
    private static final Duration NULL_CACHE_TTL = Duration.ofSeconds(60);

    /** 缓存 key 前缀，避免和别的业务键冲突。 */
    private static final String CACHE_KEY_PREFIX = "item:";

    private final ItemMapper itemMapper;

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public ItemDTO queryById(Long id) {
        if (id == null) {
            throw new BadRequestException("商品 id 不能为空");
        }
        String key = cacheKey(id);
        Item item = readCache(key);
        if (item == null) {
            item = itemMapper.selectById(id);
            if (item == null) {
                // 数据库也没有：写空值占位，短时间内不再打库
                writeCache(key, new Item(), NULL_CACHE_TTL);
                throw new BadRequestException("商品不存在：" + id);
            }
            writeCache(key, item, ITEM_CACHE_TTL);
        }
        if (item.getId() == null) {
            // 命中的是空值占位
            throw new BadRequestException("商品不存在：" + id);
        }
        return toDTO(item);
    }

    @Override
    public List<ItemDTO> queryByIds(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return List.of();
        }
        List<Long> distinctIds = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (distinctIds.isEmpty()) {
            return List.of();
        }
        // 一次 multiGet 取回所有 key，避免逐条 get 造成 N 次网络往返
        List<Object> cached = multiReadCache(distinctIds.stream().map(this::cacheKey).toList());
        List<ItemDTO> items = new ArrayList<>(distinctIds.size());
        for (int i = 0; i < distinctIds.size(); i++) {
            Long id = distinctIds.get(i);
            Item item = cached == null || i >= cached.size() ? null : (Item) cached.get(i);
            if (item == null) {
                item = itemMapper.selectById(id);
                if (item == null) {
                    writeCache(cacheKey(id), new Item(), NULL_CACHE_TTL);
                    continue;
                }
                writeCache(cacheKey(id), item, ITEM_CACHE_TTL);
            }
            // 空值占位（id 为 null）不返回给调用方
            if (item.getId() != null) {
                items.add(toDTO(item));
            }
        }
        return items;
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
            // 服务层再校验一次：本方法除 Feign 入口外也可能被内部逻辑直接调用
            if (detail == null || detail.itemId() == null) {
                throw new BadRequestException("商品 id 不能为空");
            }
            if (detail.num() == null || detail.num() < 1) {
                throw new BadRequestException("扣减数量必须大于 0：" + detail.itemId());
            }
            int rows = itemMapper.deductStock(detail.itemId(), detail.num());
            if (rows == 0) {
                // 抛出异常让本地事务与全局事务一起回滚
                throw new BizIllegalException("商品库存不足：" + detail.itemId());
            }
            // 库存已变，必须让缓存失效，否则最长 60 分钟内读到的都是旧库存
            deleteCache(cacheKey(detail.itemId()));
        }
        log.info("扣减库存成功，共 {} 个商品", details.size());
    }

    private String cacheKey(Long id) {
        return CACHE_KEY_PREFIX + id;
    }

    /**
     * 读缓存。
     *
     * <p>缓存只是加速手段，Redis 不可用时降级为直接查库，不能让商品查询整体不可用。
     */
    private Item readCache(String key) {
        try {
            return (Item) redisTemplate.opsForValue().get(key);
        } catch (DataAccessException e) {
            log.warn("读取商品缓存失败，降级为直接查库：key={}, msg={}", key, e.getMessage());
            return null;
        }
    }

    private List<Object> multiReadCache(List<String> keys) {
        try {
            return redisTemplate.opsForValue().multiGet(keys);
        } catch (DataAccessException e) {
            log.warn("批量读取商品缓存失败，降级为直接查库：keys={}, msg={}", keys, e.getMessage());
            return null;
        }
    }

    private void writeCache(String key, Item item, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, item, ttl);
        } catch (DataAccessException e) {
            log.warn("写入商品缓存失败，忽略：key={}, msg={}", key, e.getMessage());
        }
    }

    private void deleteCache(String key) {
        try {
            redisTemplate.delete(key);
        } catch (DataAccessException e) {
            log.warn("删除商品缓存失败，忽略：key={}, msg={}", key, e.getMessage());
        }
    }

    /** PO → 跨服务 DTO（字段与 ItemVO 对齐：id/name/price/image/spec/stock/sold）。 */
    private ItemDTO toDTO(Item item) {
        return new ItemDTO(item.getId(), item.getName(), item.getPrice(), item.getImage(),
                item.getSpec(), item.getStock(), item.getSold());
    }

    private ItemVO toVO(Item item) {
        return new ItemVO(item.getId(), item.getName(), item.getPrice(), item.getImage(),
                item.getSpec(), item.getStock(), item.getSold());
    }
}
