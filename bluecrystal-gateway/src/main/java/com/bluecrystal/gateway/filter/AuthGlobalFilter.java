package com.bluecrystal.gateway.filter;

import com.bluecrystal.common.domain.R;
import com.bluecrystal.common.exception.UnauthorizedException;
import com.bluecrystal.common.utils.UserContext;
import com.bluecrystal.gateway.config.AuthProperties;
import com.bluecrystal.gateway.util.JwtTool;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 全局鉴权过滤器：白名单直接放行，其余请求校验 JWT 并注入 {@code X-User-Id}。
 *
 * <p>安全要点：客户端自带的 {@code X-User-Id} 一律先删除，只信任网关验签后写入的值，
 * 避免外部伪造用户身份直连下游服务。
 */
@Slf4j
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private static final PathPatternParser PATH_PATTERN_PARSER = PathPatternParser.defaultInstance;

    private final JwtTool jwtTool;
    private final ObjectMapper objectMapper;
    private final List<PathPattern> excludePatterns;
    private final Map<String, PathPattern> patternCache = new ConcurrentHashMap<>();

    public AuthGlobalFilter(JwtTool jwtTool, AuthProperties authProperties, ObjectMapper objectMapper) {
        this.jwtTool = jwtTool;
        this.objectMapper = objectMapper;
        this.excludePatterns = authProperties.excludePaths().stream()
                .map(this::parsePattern)
                .toList();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // 1. 无论是否鉴权，都先剥离伪造的 X-User-Id
        ServerHttpRequest sanitized = request.mutate()
                .headers(headers -> headers.remove(UserContext.USER_HEADER))
                .build();

        // 2. 白名单放行
        if (isExcluded(path)) {
            return chain.filter(exchange.mutate().request(sanitized).build());
        }

        // 3. 校验 token
        Long userId;
        try {
            userId = jwtTool.parseToken(resolveToken(request));
        } catch (UnauthorizedException e) {
            log.debug("鉴权失败：path={}, msg={}", path, e.getMessage());
            return unauthorized(exchange.getResponse(), e.getMessage());
        }

        // 4. 注入用户身份后放行
        ServerHttpRequest authenticated = sanitized.mutate()
                .header(UserContext.USER_HEADER, String.valueOf(userId))
                .build();
        return chain.filter(exchange.mutate().request(authenticated).build());
    }

    private String resolveToken(ServerHttpRequest request) {
        String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization != null && !authorization.isBlank()) {
            return authorization;
        }
        // 兼容从查询参数或自定义头取 token 的老调用方式
        String header = request.getHeaders().getFirst("token");
        return header != null ? header : request.getQueryParams().getFirst("token");
    }

    private boolean isExcluded(String path) {
        return excludePatterns.stream().anyMatch(pattern -> pattern.matches(
                org.springframework.http.server.PathContainer.parsePath(path)));
    }

    private PathPattern parsePattern(String pattern) {
        return patternCache.computeIfAbsent(pattern, PATH_PATTERN_PARSER::parse);
    }

    private Mono<Void> unauthorized(ServerHttpResponse response, String message) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body;
        try {
            body = objectMapper.writeValueAsBytes(R.error(401, message));
        } catch (JsonProcessingException e) {
            body = "{\"code\":401,\"msg\":\"未登录\",\"data\":null}".getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = response.bufferFactory().wrap(body);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }
}
