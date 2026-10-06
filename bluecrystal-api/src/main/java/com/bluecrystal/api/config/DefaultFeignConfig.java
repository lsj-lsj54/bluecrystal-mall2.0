package com.bluecrystal.api.config;

import com.bluecrystal.common.utils.UserContext;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign 默认配置：把当前登录用户透传到下游服务。
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
}
