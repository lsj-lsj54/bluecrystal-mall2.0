package com.bluecrystal.pay.service.impl;

import com.bluecrystal.api.client.TradeClient;
import com.bluecrystal.api.client.UserClient;
import com.bluecrystal.api.dto.OrderDTO;
import com.bluecrystal.common.domain.R;
import com.bluecrystal.common.exception.BizIllegalException;
import com.bluecrystal.common.exception.ForbiddenException;
import com.bluecrystal.common.utils.UserContext;
import com.bluecrystal.pay.domain.dto.PayOrderFormDTO;
import com.bluecrystal.pay.mapper.PayOrderMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayOrderServiceImplTest {

    @Mock
    private PayOrderMapper payOrderMapper;

    @Mock
    private UserClient userClient;

    @Mock
    private TradeClient tradeClient;

    @InjectMocks
    private PayOrderServiceImpl payOrderService;

    @AfterEach
    void clearContext() {
        UserContext.clear();
    }

    @Test
    @DisplayName("支付金额与订单金额不一致时必须拒绝，且不扣款")
    void applyPayShouldRejectAmountMismatch() {
        UserContext.setUser(1L);
        // 订单真实金额 199900 分，只付 1 分
        when(tradeClient.queryOrderById(100L)).thenReturn(R.ok(new OrderDTO(100L, 1L, 199900, 1)));

        assertThrows(BizIllegalException.class,
                () -> payOrderService.applyPay(new PayOrderFormDTO(100L, 1, "balance")));
        verifyNoInteractions(userClient, payOrderMapper);
    }

    @Test
    @DisplayName("已支付/已关闭的订单不能重复支付")
    void applyPayShouldRejectNonWaitPayOrder() {
        UserContext.setUser(1L);
        when(tradeClient.queryOrderById(100L)).thenReturn(R.ok(new OrderDTO(100L, 1L, 100, 2)));

        assertThrows(BizIllegalException.class,
                () -> payOrderService.applyPay(new PayOrderFormDTO(100L, 100, "balance")));
        verifyNoInteractions(userClient);
    }

    @Test
    @DisplayName("不能支付别人的订单")
    void applyPayShouldRejectOtherUsersOrder() {
        UserContext.setUser(2L);
        when(tradeClient.queryOrderById(100L)).thenReturn(R.ok(new OrderDTO(100L, 1L, 100, 1)));

        assertThrows(ForbiddenException.class,
                () -> payOrderService.applyPay(new PayOrderFormDTO(100L, 100, "balance")));
        verifyNoInteractions(userClient);
    }
}
