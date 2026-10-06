package com.bluecrystal.common.web;

import com.bluecrystal.common.utils.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 把网关写入的 {@code X-User-Id} 请求头落到 {@link UserContext}，请求结束后清理。
 */
@Slf4j
public class UserInfoInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String userId = request.getHeader(UserContext.USER_HEADER);
        if (userId != null && !userId.isBlank()) {
            try {
                UserContext.setUser(Long.valueOf(userId.trim()));
            } catch (NumberFormatException e) {
                log.warn("非法的 {} 请求头：{}", UserContext.USER_HEADER, userId);
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }
}
