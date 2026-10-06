package com.bluecrystal.trade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecrystal.api.client.CartClient;
import com.bluecrystal.api.client.ItemClient;
import com.bluecrystal.api.dto.ItemDTO;
import com.bluecrystal.api.dto.OrderDTO;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

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
     *
     * <p>金额一律以 item-service 返回的价格计算，**不信任前端传上来的价格**，
     * 否则调用方把 price 改成 1 就能一分钱下单。
     */
    @Override
    @GlobalTransactional(name = "create-order", rollbackFor = Exception.class)
    public OrderVO createOrder(OrderFormDTO form) {
        // 1. 当前登录用户（网关透传 X-User-Id，UserInfoInterceptor 写入上下文）
        Long userId = UserContext.requireUser();
        List<OrderDetailDTO> formDetails = form.details();
        if (CollectionUtils.isEmpty(formDetails)) {
            throw new BadRequestException("订单明细不能为空");
        }

        // 2. 校验商品 id 与数量
        List<Long> itemIds = new ArrayList<>(formDetails.size());
        for (OrderDetailDTO detail : formDetails) {
            if (detail == null || detail.itemId() == null) {
                throw new BadRequestException("商品 id 不能为空");
            }
            if (detail.num() == null || detail.num() < 1) {
                throw new BadRequestException("购买数量必须大于 0：" + detail.itemId());
            }
            itemIds.add(detail.itemId());
        }
        List<Long> distinctItemIds = itemIds.stream().distinct().toList();

        // 3. 回查商品服务，用服务端价格生成下单快照
        R<List<ItemDTO>> itemResult = itemClient.queryItemsByIds(distinctItemIds);
        if (itemResult == null || !itemResult.success() || CollectionUtils.isEmpty(itemResult.data())) {
            throw new BizIllegalException("商品信息查询失败");
        }
        Map<Long, ItemDTO> itemMap = itemResult.data().stream()
                .filter(item -> item != null && item.id() != null)
                .collect(Collectors.toMap(ItemDTO::id, Function.identity(), (first, second) -> first));

        List<OrderDetailDTO> snapshots = new ArrayList<>(formDetails.size());
        int totalFee = 0;
        for (OrderDetailDTO detail : formDetails) {
            ItemDTO item = itemMap.get(detail.itemId());
            if (item == null) {
                throw new BizIllegalException("商品不存在或已下架：" + detail.itemId());
            }
            int price = item.price() == null ? 0 : item.price();
            totalFee += price * detail.num();
            snapshots.add(new OrderDetailDTO(item.id(), detail.num(), item.name(),
                    price, item.image(), item.spec()));
        }

        // 4. 扣减库存：Feign 调用 item-service，作为全局事务分支参与回滚
        R<Void> deductResult = itemClient.deductStock(snapshots);
        if (deductResult == null || !deductResult.success()) {
            throw new BizIllegalException(deductResult == null ? "扣减库存失败" : deductResult.msg());
        }

        // 5. 保存订单，订单号由雪花算法生成并回填到 id
        LocalDateTime now = LocalDateTime.now();
        Order order = new Order();
        order.setTotalFee(totalFee);
        order.setPaymentType(form.paymentType());
        order.setUserId(userId);
        order.setStatus(STATUS_UN_PAID);
        order.setCreateTime(now);
        order.setUpdateTime(now);
        orderMapper.insert(order);

        // 6. 保存订单明细（以下单时回查到的商品信息为准）
        for (OrderDetailDTO snapshot : snapshots) {
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrderId(order.getId());
            orderDetail.setItemId(snapshot.itemId());
            orderDetail.setNum(snapshot.num());
            orderDetail.setName(snapshot.name());
            orderDetail.setSpec(snapshot.spec());
            orderDetail.setPrice(snapshot.price());
            orderDetail.setImage(snapshot.image());
            orderDetail.setCreateTime(now);
            orderDetail.setUpdateTime(now);
            orderDetailMapper.insert(orderDetail);
        }

        // 7. 清理购物车中已下单的商品
        R<Void> cartResult = cartClient.removeByItemIds(distinctItemIds);
        if (cartResult == null || !cartResult.success()) {
            throw new BizIllegalException(cartResult == null ? "清理购物车失败" : cartResult.msg());
        }

        log.info("下单成功，订单号：{}，用户：{}，金额：{} 分", order.getId(), userId, totalFee);
        // TODO 订单超时未支付需要自动关闭（延迟消息 / 定时任务）
        return toVO(order, snapshots);
    }

    @Override
    public OrderVO queryById(Long orderId) {
        Order order = requireOwnOrder(orderId);
        List<OrderDetail> orderDetails = orderDetailMapper.selectList(
                new LambdaQueryWrapper<OrderDetail>().eq(OrderDetail::getOrderId, orderId));
        List<OrderDetailDTO> details = orderDetails.stream()
                .map(detail -> new OrderDetailDTO(detail.getItemId(), detail.getNum(), detail.getName(),
                        detail.getPrice(), detail.getImage(), detail.getSpec()))
                .toList();
        return toVO(order, details);
    }

    @Override
    public OrderDTO queryForPayment(Long orderId) {
        Order order = requireOwnOrder(orderId);
        return new OrderDTO(order.getId(), order.getUserId(), order.getTotalFee(), order.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markPaid(Long orderId) {
        Order order = requireOwnOrder(orderId);
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

    /** 取出订单并校验归属：不存在抛 400，不属于当前登录用户抛 403。 */
    private Order requireOwnOrder(Long orderId) {
        if (orderId == null) {
            throw new BadRequestException("订单号不能为空");
        }
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BadRequestException("订单不存在：" + orderId);
        }
        if (!Objects.equals(order.getUserId(), UserContext.requireUser())) {
            throw new ForbiddenException("无权操作该订单：" + orderId);
        }
        return order;
    }

    private OrderVO toVO(Order order, List<OrderDetailDTO> details) {
        return new OrderVO(order.getId(), order.getTotalFee(), order.getStatus(),
                order.getPaymentType(), order.getCreateTime(), details);
    }
}
