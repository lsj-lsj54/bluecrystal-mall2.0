package com.bluecrystal.user.domain.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录入参。
 *
 * @param username 用户名
 * @param password 明文密码，服务端用 BCrypt 与库中密文比对
 */
public record LoginFormDTO(
        @NotBlank(message = "用户名不能为空") String username,
        @NotBlank(message = "密码不能为空") String password) {
}
