package com.bluecrystal.api.config;

import org.apache.seata.core.context.RootContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import feign.RequestInterceptor;

/**
 * Seata 全局事务传播：把当前分支的 XID 透传给下游服务。
 *
 * <p>未引入 Seata 的服务（classpath 上没有 RootContext）会自动跳过这段配置。
 */
@Configuration
@ConditionalOnClass(RootContext.class)
public class SeataFeignConfig {

    public static final String XID_HEADER = "TX_XID";

    @Bean
    public RequestInterceptor seataXidRequestInterceptor() {
        return template -> {
            String xid = RootContext.getXID();
            if (xid != null && !xid.isBlank()) {
                template.header(XID_HEADER, xid);
            }
        };
    }
}
