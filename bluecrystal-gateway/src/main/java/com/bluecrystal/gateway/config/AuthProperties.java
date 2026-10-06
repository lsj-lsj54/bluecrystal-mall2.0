package com.bluecrystal.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 鉴权白名单配置：命中 exclude-paths 的请求不校验 token。
 *
 * @param excludePaths 免登录路径，支持 PathPattern 语法，如 /items/**
 */
@ConfigurationProperties(prefix = "bc.auth")
public record AuthProperties(List<String> excludePaths) {

    public AuthProperties {
        excludePaths = excludePaths == null ? List.of() : List.copyOf(excludePaths);
    }
}
