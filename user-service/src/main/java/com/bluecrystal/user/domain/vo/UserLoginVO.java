package com.bluecrystal.user.domain.vo;

/**
 * 登录结果视图对象。
 *
 * @param token 网关可验签的 JWT
 * @param userId 用户 id
 * @param username 用户名
 * @param balance 余额，单位：分
 */
public record UserLoginVO(String token, Long userId, String username, Integer balance) {
}
