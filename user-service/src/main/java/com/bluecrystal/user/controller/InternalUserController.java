package com.bluecrystal.user.controller;

import com.bluecrystal.common.domain.R;
import com.bluecrystal.user.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户服务的内部接口。
 *
 * <p>路径统一挂在 {@code /internal/**}：网关没有为这个前缀配置路由，外部拿不到；
 * 服务层还会校验「被扣款的用户必须是当前登录用户」，双保险防止越权扣别人的余额。
 */
@Tag(name = "用户内部接口")
@Validated
@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final IUserService userService;

    @Operation(summary = "扣减余额（由 pay-service 在全局事务中调用）")
    @PostMapping("/{userId}/balance/deduct")
    public R<Void> deductBalance(@PathVariable("userId") Long userId,
                                 @RequestParam("amount") Integer amount) {
        userService.deductBalance(userId, amount);
        return R.ok();
    }
}
