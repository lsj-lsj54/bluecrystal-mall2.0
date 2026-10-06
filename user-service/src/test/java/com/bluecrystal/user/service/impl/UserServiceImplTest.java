package com.bluecrystal.user.service.impl;

import com.bluecrystal.common.exception.BadRequestException;
import com.bluecrystal.common.exception.BizIllegalException;
import com.bluecrystal.common.exception.ForbiddenException;
import com.bluecrystal.common.utils.UserContext;
import com.bluecrystal.user.mapper.UserMapper;
import com.bluecrystal.user.utils.JwtTool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private JwtTool jwtTool;

    @InjectMocks
    private UserServiceImpl userService;

    @AfterEach
    void clearContext() {
        UserContext.clear();
    }

    @Test
    @DisplayName("不能扣别人的余额：被扣款人必须是当前登录用户")
    void deductBalanceShouldRejectOtherUser() {
        UserContext.setUser(2L);

        assertThrows(ForbiddenException.class, () -> userService.deductBalance(1L, 100));
        verifyNoInteractions(userMapper);
    }

    @Test
    @DisplayName("金额必须为正数")
    void deductBalanceShouldRejectNonPositiveAmount() {
        UserContext.setUser(1L);

        assertThrows(BadRequestException.class, () -> userService.deductBalance(1L, 0));
        assertThrows(BadRequestException.class, () -> userService.deductBalance(1L, -1));
        assertThrows(BadRequestException.class, () -> userService.deductBalance(1L, null));
        verifyNoInteractions(userMapper);
    }

    @Test
    @DisplayName("条件更新影响 0 行代表余额不足")
    void deductBalanceShouldFailWhenBalanceNotEnough() {
        UserContext.setUser(1L);
        when(userMapper.deductBalance(1L, 100)).thenReturn(0);

        assertThrows(BizIllegalException.class, () -> userService.deductBalance(1L, 100));
    }

    @Test
    @DisplayName("本人扣款正常走到数据库")
    void deductBalanceShouldWorkForSelf() {
        UserContext.setUser(1L);
        when(userMapper.deductBalance(1L, 100)).thenReturn(1);

        userService.deductBalance(1L, 100);

        verify(userMapper).deductBalance(1L, 100);
    }
}
