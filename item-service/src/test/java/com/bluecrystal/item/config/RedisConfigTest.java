package com.bluecrystal.item.config;

import com.bluecrystal.item.domain.po.Item;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 回归测试：商品实体含 {@link LocalDateTime}，如果 Redis 序列化器没注册 JavaTimeModule，
 * 写入缓存时会抛 {@code InvalidDefinitionException: Java 8 date/time type ... not supported by default}。
 */
class RedisConfigTest {

    @Test
    @DisplayName("含 LocalDateTime 的商品可以正常序列化/反序列化")
    void shouldSerializeItemWithLocalDateTime() {
        GenericJackson2JsonRedisSerializer serializer =
                new GenericJackson2JsonRedisSerializer(RedisConfig.redisObjectMapper());

        Item item = new Item();
        item.setId(1L);
        item.setName("Demo Phone");
        item.setPrice(199900);
        item.setStock(100);
        item.setCreateTime(LocalDateTime.of(2026, 10, 6, 12, 30, 45));
        item.setUpdateTime(LocalDateTime.of(2026, 10, 6, 12, 31, 0));

        byte[] bytes = serializer.serialize(item);
        assertNotNull(bytes);

        Object restored = serializer.deserialize(bytes);
        Item cached = assertInstanceOf(Item.class, restored, "反序列化后应还原成 Item 而不是 LinkedHashMap");
        assertEquals(1L, cached.getId());
        assertEquals(LocalDateTime.of(2026, 10, 6, 12, 30, 45), cached.getCreateTime());
    }
}
