package com.bluecrystal.user.controller;

import com.bluecrystal.common.domain.R;
import com.bluecrystal.user.domain.dto.LoginFormDTO;
import com.bluecrystal.user.domain.vo.UserLoginVO;
import com.bluecrystal.user.domain.vo.UserVO;
import com.bluecrystal.user.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户接口")
@Validated
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final IUserService userService;

    @Operation(summary = "登录并签发 token")
    @PostMapping("/login")
    public R<UserLoginVO> login(@Valid @RequestBody LoginFormDTO form) {
        return R.ok(userService.login(form));
    }

    @Operation(summary = "查询当前登录用户")
    @GetMapping("/me")
    public R<UserVO> me() {
        return R.ok(userService.currentUser());
    }

    // 注意：扣余额属于内部能力，已挪到 /internal/users/**（InternalUserController），
    // 不再通过网关对外暴露，避免任何登录用户都能扣别人余额。
}
