package com.bluecrystal.trade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecrystal.api.client.CartClient;
import com.bluecrystal.api.client.ItemClient;
import com.bluecrystal.api.dto.OrderDetailDTO;
import com.bluecrystal.common.domain.R;
import com.bluecrystal.common.exception.BadRequestException;
import com.bluecrystal.common.exception.BizIllegalException;
import com.bluecrystal.common.exception.ForbiddenException;
import com.bluecrystal.common.utils.UserContext;
import com.bluecrystal.trade.domain.dto.OrderFormDTO;
import com.bluecrystal.trade.domain.po.Order;
import com.bluecrystal.trade.domain.po.OrderDetail;
import com.bluecrystal.trade.domain.vo.OrderVO;
import com.bluecrystal.trade.mapper.OrderDetailMapper;
import com.bluecrystal.trade.mapper.OrderMapper;
import com.bluecrystal.trade.service.IOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.seata.spring.annotation.GlobalTransactional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements IOrderService {

    /** 订单状态：待支付。 */
    private static final int STATUS_UN_PAID = 1;

    /** 订单状态：已支付。 */
    private static final int STATUS_PAID = 2;

    private final OrderMapper orderMapper;
    private final OrderDetailMapper orderDetailMapper;
    private final ItemClient itemClient;
    private final CartClient cartClient;

    /**
     * 创建订单。
     *
     * <p>这是 Seata 全局事务的发起方（TM）：{@code @GlobalTransactional} 会开启全局事务，
     * 之后对 item-service 的扣库存调用与本地订单落库都会注册为分支事务，
     * 任意一步抛出异常都会让已完成的远程分支一起回滚。
     */
    @Override
    @GlobalTransactional(name = "create-order", rollbackFor = Exception.class)
    public OrderVO createOrder(OrderFormDTO form) {
        // 1. 当前登录用户（网关透传 X-User-Id，UserInfoInterceptor 写入上下文）
        Long userId = UserContext.requireUser();
        List<OrderDetailDTO> details = form.details();
        if (CollectionUtils.isEmpty(details)) {
            throw new BadRequestException("订单明细不能为空");
        }

        // 2. 计算订单总金额，单位：分
        int totalFee = details.stream()
                .mapToInt(detail -> detail.price() * detail.num())
                .sum();

        // 3. 扣减库存：Feign 调用 item-service，作为全局事务分支参与回滚
        R<Void> deductResult = itemClient.deductStock(details);
        if (!deductResult.success()) {
            throw new BizIllegalException("扣减库存失败：" + deductResult.msg());
        }

        // 4. 保存订单，订单号由雪花算法生成并回填到 id
        LocalDateTime now = LocalDateTime.now();
        Order order = new Order();
        order.setTotalFee(totalFee);
        order.setPaymentType(form.paymentType());
        order.setUserId(userId);
        order.setStatus(STATUS_UN_PAID);
        order.setCreateTime(now);
        order.setUpdateTime(now);
        orderMapper.insert(order);

        // 5. 保存订单明细（商品信息下单快照）
        for (OrderDetailDTO detail : details) {
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrderId(order.getId());
            orderDetail.setItemId(detail.itemId());
            orderDetail.setNum(detail.num());
            orderDetail.setName(detail.name());
            orderDetail.setSpec(detail.spec());
            orderDetail.setPrice(detail.price());
            orderDetail.setImage(detail.image());
            orderDetail.setCreateTime(now);
            orderDetail.setUpdateTime(now);
            orderDetailMapper.insert(orderDetail);
        }

        // 6. 清理购物车中已下单的商品
        List<Long> itemIds = details.stream().map(OrderDetailDTO::itemId).toList();
        R<Void> cartResult = cartClient.removeByItemIds(itemIds);
        if (!cartResult.success()) {
            throw new BizIllegalException("清理购物车失败：" + cartResult.msg());
        }

        log.info("下单成功，订单号：{}，用户：{}，金额：{} 分", order.getId(), userId, totalFee);
        // TODO 订单超时未支付需要自动关闭（延迟消息 / 定时任务）
        return toVO(order, details);
    }

    @Override
    public OrderVO queryById(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BadRequestException("订单不存在：" + orderId);
        }
        // 只允许订单所属用户查看，防止越权访问他人订单
        if (!Objects.equals(order.getUserId(), UserContext.requireUser())) {
            throw new ForbiddenException("无权查看该订单：" + orderId);
        }
        List<OrderDetail> orderDetails = orderDetailMapper.selectList(
                new LambdaQueryWrapper<OrderDetail>().eq(OrderDetail::getOrderId, orderId));
        List<OrderDetailDTO> details = orderDetails.stream()
                .map(detail -> new OrderDetailDTO(detail.getItemId(), detail.getNum(), detail.getName(),
                        detail.getPrice(), detail.getImage(), detail.getSpec()))
                .toList();
        return toVO(order, details);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markPaid(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BadRequestException("订单不存在：" + orderId);
        }
        if (!Objects.equals(order.getStatus(), STATUS_UN_PAID)) {
            // 支付回调可能重复投递，非待支付状态直接拒绝
            throw new BizIllegalException("订单状态不允许支付：" + order.getStatus());
        }
        LocalDateTime now = LocalDateTime.now();
        Order update = new Order();
        update.setId(orderId);
        update.setStatus(STATUS_PAID);
        update.setPayTime(now);
        update.setUpdateTime(now);
        orderMapper.updateById(update);
        // TODO 支付成功后通知发货 / 推送状态变更消息
        log.info("订单支付成功，订单号：{}", orderId);
    }

    private OrderVO toVO(Order order, List<OrderDetailDTO> details) {
        return new OrderVO(order.getId(), order.getTotalFee(), order.getStatus(),
                order.getPaymentType(), order.getCreateTime(), details);
    }
}
