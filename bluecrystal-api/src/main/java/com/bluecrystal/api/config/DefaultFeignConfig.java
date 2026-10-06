package com.bluecrystal.api.config;

import com.bluecrystal.common.utils.UserContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign 默认配置：
 * <ul>
 *   <li>把当前登录用户（{@code X-User-Id}）透传给下游服务；</li>
 *   <li>用 {@link BluecrystalFeignErrorDecoder} 保留下游的业务错误码与提示。</li>
 * </ul>
 *
 * <p>用法：在各服务的启动类上声明
 * {@code @EnableFeignClients(basePackages = "com.bluecrystal.api.client", defaultConfiguration = DefaultFeignConfig.class)}。
 */
@Configuration
public class DefaultFeignConfig {

    @Bean
    public RequestInterceptor userInfoRequestInterceptor() {
        return template -> {
            Long userId = UserContext.getUser();
            if (userId != null && !template.headers().containsKey(UserContext.USER_HEADER)) {
                template.header(UserContext.USER_HEADER, String.valueOf(userId));
            }
        };
    }

    @Bean
    public ErrorDecoder bluecrystalFeignErrorDecoder(ObjectMapper objectMapper) {
        return new BluecrystalFeignErrorDecoder(objectMapper);
    }
}
