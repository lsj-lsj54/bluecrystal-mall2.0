package com.bluecrystal.trade.service.impl;

import com.bluecrystal.api.client.CartClient;
import com.bluecrystal.api.client.ItemClient;
import com.bluecrystal.api.dto.ItemDTO;
import com.bluecrystal.api.dto.OrderDetailDTO;
import com.bluecrystal.common.domain.R;
import com.bluecrystal.common.exception.BizIllegalException;
import com.bluecrystal.common.utils.UserContext;
import com.bluecrystal.trade.domain.dto.OrderFormDTO;
import com.bluecrystal.trade.domain.po.Order;
import com.bluecrystal.trade.domain.vo.OrderVO;
import com.bluecrystal.trade.mapper.OrderDetailMapper;
import com.bluecrystal.trade.mapper.OrderMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderDetailMapper orderDetailMapper;

    @Mock
    private ItemClient itemClient;

    @Mock
    private CartClient cartClient;

    @InjectMocks
    private OrderServiceImpl orderService;

    @AfterEach
    void clearContext() {
        UserContext.clear();
    }

    @Test
    @DisplayName("下单金额以商品服务价格为准，不信任前端传的 price")
    void createOrderShouldUseServerSidePrice() {
        UserContext.setUser(1L);
        // 前端谎报 price=1，商品服务返回的真实价格是 1999 分
        OrderDetailDTO clientDetail = new OrderDetailDTO(10L, 2, "手机", 1, "img", "spec");
        when(itemClient.queryItemsByIds(List.of(10L)))
                .thenReturn(R.ok(List.of(new ItemDTO(10L, "手机", 1999, "img", "spec", 100, 0))));
        when(itemClient.deductStock(anyList())).thenReturn(R.ok());
        when(cartClient.removeByItemIds(anyList())).thenReturn(R.ok());

        OrderVO vo = orderService.createOrder(new OrderFormDTO(List.of(clientDetail), 3));

        assertEquals(3998, vo.totalFee(), "应为 1999 * 2，而不是前端传的 1 * 2");
        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderMapper).insert(captor.capture());
        assertEquals(3998, captor.getValue().getTotalFee());
    }

    @Test
    @DisplayName("商品不存在时直接失败，不会扣库存")
    void createOrderShouldFailWhenItemMissing() {
        UserContext.setUser(1L);
        OrderDetailDTO detail = new OrderDetailDTO(10L, 1, "手机", 1999, "img", "spec");
        when(itemClient.queryItemsByIds(List.of(10L))).thenReturn(R.ok(List.of()));

        assertThrows(BizIllegalException.class,
                () -> orderService.createOrder(new OrderFormDTO(List.of(detail), 3)));
    }

    @Test
    @DisplayName("数量非法时拒绝下单")
    void createOrderShouldRejectInvalidNum() {
        UserContext.setUser(1L);
        OrderDetailDTO detail = new OrderDetailDTO(10L, 0, "手机", 1999, "img", "spec");

        assertThrows(com.bluecrystal.common.exception.BadRequestException.class,
                () -> orderService.createOrder(new OrderFormDTO(List.of(detail), 3)));
    }

    @Test
    @DisplayName("不能操作别人的订单：标记已支付前校验归属")
    void markPaidShouldRejectOtherUsersOrder() {
        UserContext.setUser(2L);
        Order order = new Order();
        order.setId(100L);
        order.setUserId(1L);
        order.setStatus(1);
        when(orderMapper.selectById(100L)).thenReturn(order);

        assertThrows(com.bluecrystal.common.exception.ForbiddenException.class,
                () -> orderService.markPaid(100L));
        verify(orderMapper, org.mockito.Mockito.never()).updateById(any(Order.class));
    }
}
