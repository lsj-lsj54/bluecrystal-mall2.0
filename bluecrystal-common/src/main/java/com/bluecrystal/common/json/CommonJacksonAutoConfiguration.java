package com.bluecrystal.common.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;

import java.math.BigInteger;
import java.util.TimeZone;

/**
 * JSON 序列化约定：Long / BigInteger 输出为字符串，避免前端 JS 精度丢失。
 */
@AutoConfiguration
@ConditionalOnClass(ObjectMapper.class)
public class CommonJacksonAutoConfiguration {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer bluecrystalJacksonCustomizer() {
        return builder -> builder
                .serializerByType(Long.class, ToStringSerializer.instance)
                .serializerByType(Long.TYPE, ToStringSerializer.instance)
                .serializerByType(BigInteger.class, ToStringSerializer.instance)
                .simpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .timeZone(TimeZone.getTimeZone("Asia/Shanghai"));
    }
}
