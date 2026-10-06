package com.bluecrystal.gateway.util;

import com.bluecrystal.common.exception.UnauthorizedException;
import com.bluecrystal.gateway.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * 网关侧的 JWT 校验工具（只负责解析与验签）。
 */
@Component
public class JwtTool {

    /** 自定义 claim：用户 id。 */
    public static final String CLAIM_USER = "user";

    private static final String BEARER_PREFIX = "Bearer ";

    private final SecretKey key;

    public JwtTool(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
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
