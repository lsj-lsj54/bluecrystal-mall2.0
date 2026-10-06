package com.bluecrystal.user.utils;

import com.bluecrystal.common.exception.UnauthorizedException;
import com.bluecrystal.user.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * 用户服务侧的 JWT 工具：负责签发 token，并保留与网关一致的解析能力便于自测。
 *
 * <p>签发与校验共用同一把 HS256 密钥（{@code bc.jwt.secret}），网关只做验签。
 */
@Component
public class JwtTool {

    /** 自定义 claim：用户 id，必须与网关 JwtTool 保持一致。 */
    public static final String CLAIM_USER = "user";

    private static final String BEARER_PREFIX = "Bearer ";

    private final SecretKey key;

    private final JwtProperties properties;

    public JwtTool(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 签发 token：把用户 id 写入 {@code user} claim，并按 {@code bc.jwt.ttl} 设置过期时间。
     *
     * @param userId 用户 id
     * @return 紧凑格式的 JWS 字符串
     */
    public String createToken(Long userId) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + properties.ttl().toMillis());
        return Jwts.builder()
                .claim(CLAIM_USER, userId)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 校验并解析 token，返回用户 id。
     *
     * @throws UnauthorizedException token 缺失、被篡改或已过期
     */
    public Long parseToken(String token) {
        if (token == null || token.isBlank()) {
            throw new UnauthorizedException("未登录");
        }
        String jws = token.startsWith(BEARER_PREFIX) ? token.substring(BEARER_PREFIX.length()) : token;
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(jws.trim())
                    .getPayload();
            Object user = claims.get(CLAIM_USER);
            if (user == null) {
                throw new UnauthorizedException("无效的 token");
            }
            return Long.valueOf(user.toString());
        } catch (ExpiredJwtException e) {
            throw new UnauthorizedException("token 已过期", e);
        } catch (JwtException | IllegalArgumentException e) {
            throw new UnauthorizedException("无效的 token", e);
        }
    }
}
