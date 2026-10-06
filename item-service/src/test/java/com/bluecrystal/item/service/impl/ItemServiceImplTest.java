package com.bluecrystal.item.service.impl;

import com.bluecrystal.common.exception.BadRequestException;
import com.bluecrystal.common.exception.BizIllegalException;
import com.bluecrystal.item.domain.dto.ItemStockDeductDTO;
import com.bluecrystal.item.mapper.ItemMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemServiceImplTest {

    @Mock
    private ItemMapper itemMapper;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @InjectMocks
    private ItemServiceImpl itemService;

    @Test
    @DisplayName("负数/零数量的扣库存请求必须被拒绝（否则 SQL 会变成加库存）")
    void deductStockShouldRejectNonPositiveNum() {
        assertThrows(BadRequestException.class,
                () -> itemService.deductStock(List.of(new ItemStockDeductDTO(1L, -5))));
        assertThrows(BadRequestException.class,
                () -> itemService.deductStock(List.of(new ItemStockDeductDTO(1L, 0))));
        assertThrows(BadRequestException.class,
                () -> itemService.deductStock(List.of(new ItemStockDeductDTO(null, 1))));
        verifyNoInteractions(itemMapper);
    }

    @Test
    @DisplayName("影响行数为 0（库存不足）时抛业务异常，交给 Seata 回滚")
    void deductStockShouldFailWhenStockNotEnough() {
        when(itemMapper.deductStock(1L, 2)).thenReturn(0);

        assertThrows(BizIllegalException.class,
                () -> itemService.deductStock(List.of(new ItemStockDeductDTO(1L, 2))));
    }

    @Test
    @DisplayName("扣减成功后必须删除商品缓存，避免读到旧库存")
    void deductStockShouldEvictCache() {
        when(itemMapper.deductStock(1L, 2)).thenReturn(1);

        itemService.deductStock(List.of(new ItemStockDeductDTO(1L, 2)));

        verify(itemMapper).deductStock(1L, 2);
        verify(redisTemplate).delete("item:1");
    }
}
