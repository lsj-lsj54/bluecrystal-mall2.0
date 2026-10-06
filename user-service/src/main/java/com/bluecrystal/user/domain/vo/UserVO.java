package com.bluecrystal.user.domain.vo;

/**
 * 用户视图对象。
 *
 * <p>刻意不包含 password 字段，避免密码密文外泄。
 */
public record UserVO(Long id, String username, String phone, Integer balance, Integer status) {
}
