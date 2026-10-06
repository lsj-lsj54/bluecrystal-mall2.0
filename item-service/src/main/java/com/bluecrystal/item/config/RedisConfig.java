package com.bluecrystal.item.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 序列化配置。
 *
 * <p>关键点：{@link GenericJackson2JsonRedisSerializer} 默认用的是「裸」ObjectMapper，
 * 没有注册 {@code JavaTimeModule}，而 {@code Item} 里有 {@code LocalDateTime} 字段，
 * 直接缓存会抛 {@code InvalidDefinitionException: Java 8 date/time type ... not supported by default}。
 * 所以这里显式构造 ObjectMapper：注册 JavaTimeModule + 打开默认类型信息（反序列化时才能还原成 Item）。
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        template.setValueSerializer(new GenericJackson2JsonRedisSerializer(redisObjectMapper()));
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer(redisObjectMapper()));

        template.afterPropertiesSet();
        return template;
    }

    /** 包级可见（供单元测试直接验证序列化能力）。 */
    static ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        // 时间按 ISO-8601 字符串存，避免写成一串时间戳数字，便于用 redis-cli 直接看
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 写入类型信息，读到缓存后才能还原成具体类型而不是 LinkedHashMap
        mapper.activateDefaultTyping(
                mapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);
        return mapper;
    }
}
