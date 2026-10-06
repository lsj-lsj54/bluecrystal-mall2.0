package com.bluecrystal.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * JWT 配置。
 *
 * <p>骨架默认使用 HS256 共享密钥（网关与 user-service 必须一致），生产环境建议改为 RS256 + 密钥对。
 *
 * @param secret 签名密钥，UTF-8 编码后不得少于 32 字节
 * @param ttl token 有效期
 */
@ConfigurationProperties(prefix = "bc.jwt")
public record JwtProperties(String secret, Duration ttl) {

    public JwtProperties {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("bc.jwt.secret 必须配置且 UTF-8 编码后不少于 32 字节");
        }
        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            ttl = Duration.ofMinutes(30);
        }
    }
}
