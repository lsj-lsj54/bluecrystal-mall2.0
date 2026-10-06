package com.bluecrystal.common.utils;

/**
 * 当前登录用户上下文。
 *
 * <p>网关校验 JWT 后把用户 id 写入请求头 {@code X-User-Id}；
 * 各 MVC 服务的 {@link com.bluecrystal.common.web.UserInfoInterceptor} 负责写入本上下文，
 * 请求结束后清理，避免线程复用导致的数据串号。
 */
public final class UserContext {

    public static final String USER_HEADER = "X-User-Id";

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    private UserContext() {
    }

    public static void setUser(Long userId) {
        USER_ID.set(userId);
    }

    public static Long getUser() {
        return USER_ID.get();
    }

    /** 未登录时返回 null，需要强校验的场景请自行抛 UnauthorizedException。 */
    public static Long requireUser() {
        Long userId = USER_ID.get();
        if (userId == null) {
            throw new com.bluecrystal.common.exception.UnauthorizedException("未登录");
        }
        return userId;
    }

    public static void clear() {
        USER_ID.remove();
    }
}
