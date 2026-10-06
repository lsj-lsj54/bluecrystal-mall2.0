package com.bluecrystal.pay.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.bluecrystal.api.client.TradeClient;
import com.bluecrystal.api.client.UserClient;
import com.bluecrystal.api.dto.OrderDTO;
import com.bluecrystal.common.domain.R;
import com.bluecrystal.common.exception.BadRequestException;
import com.bluecrystal.common.exception.BizIllegalException;
import com.bluecrystal.common.exception.ForbiddenException;
import com.bluecrystal.common.utils.UserContext;
import com.bluecrystal.pay.domain.dto.PayOrderFormDTO;
import com.bluecrystal.pay.domain.po.PayOrder;
import com.bluecrystal.pay.domain.vo.PayOrderVO;
import com.bluecrystal.pay.enums.PayChannel;
import com.bluecrystal.pay.enums.PayStatus;
import com.bluecrystal.pay.mapper.PayOrderMapper;
import com.bluecrystal.pay.service.IPayOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.seata.spring.annotation.GlobalTransactional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayOrderServiceImpl implements IPayOrderService {

    /** 订单状态：待支付（与 trade-service 保持一致）。 */
    private static final int ORDER_STATUS_UN_PAID = 1;

    private final PayOrderMapper payOrderMapper;

    private final UserClient userClient;

    private final TradeClient tradeClient;

    @Override
    @GlobalTransactional(name = "apply-pay", rollbackFor = Exception.class)
    @Transactional(rollbackFor = Exception.class)
    public PayOrderVO applyPay(PayOrderFormDTO form) {
        Long userId = UserContext.requireUser();

        // TODO 后续接入支付宝/微信：按 payChannelCode 分发到各自的渠道策略实现
        //  （下单预支付、异步回调验签、主动查单），当前骨架只支持余额支付
        if (!PayChannel.BALANCE.getCode().equals(form.payChannelCode())) {
            throw new BizIllegalException("暂不支持的支付渠道");
        }

        // 1. 向订单服务核对：订单必须存在、属于当前用户、处于待支付、且金额与本次支付一致。
        //    必须在扣款之前完成，否则「1 分钱付 1999 元的订单」这类金额篡改会直接扣款成功。
        OrderDTO order = requirePayableOrder(userId, form);

        // 2. 落一条待支付的支付单
        LocalDateTime now = LocalDateTime.now();
        PayOrder payOrder = new PayOrder();
        payOrder.setBizOrderNo(order.id());
        payOrder.setBizUserId(userId);
        payOrder.setPayChannelCode(PayChannel.BALANCE.getCode());
        payOrder.setAmount(order.totalFee());
        payOrder.setStatus(PayStatus.WAIT_PAY.getValue());
        payOrder.setCreateTime(now);
        payOrder.setUpdateTime(now);
        payOrderMapper.insert(payOrder);
        log.info("支付单已创建：id={}, bizOrderNo={}, amount={} 分",
                payOrder.getId(), payOrder.getBizOrderNo(), payOrder.getAmount());

        // 3. 余额支付：扣余额（user-service 分支），失败即抛异常让全局事务回滚
        R<Void> deductResult = userClient.deductBalance(userId, payOrder.getAmount());
        if (deductResult == null || !deductResult.success()) {
            throw new BizIllegalException(deductResult == null ? "余额支付失败" : deductResult.msg());
        }

        // 4. 回写订单已支付（trade-service 分支）
        R<Void> paidResult = tradeClient.markOrderPaid(order.id());
        if (paidResult == null || !paidResult.success()) {
            throw new BizIllegalException(paidResult == null ? "回写订单支付状态失败" : paidResult.msg());
        }

        // 5. 更新支付单为已支付，并写入支付单号与支付成功时间
        PayOrder success = new PayOrder();
        success.setId(payOrder.getId());
        success.setPayOrderNo(IdWorker.getId());
        success.setStatus(PayStatus.SUCCESS.getValue());
        success.setResultCode("SUCCESS");
        success.setResultMsg("余额支付成功");
        success.setPaySuccessTime(LocalDateTime.now());
        success.setUpdateTime(LocalDateTime.now());
        payOrderMapper.updateById(success);

        log.info("余额支付成功：payOrderNo={}, bizOrderNo={}", success.getPayOrderNo(), order.id());
        return toVO(payOrderMapper.selectById(payOrder.getId()));
    }

    @Override
    public PayOrderVO queryById(Long id) {
        if (id == null) {
            throw new BadRequestException("支付单 id 不能为空");
        }
        PayOrder payOrder = payOrderMapper.selectById(id);
        if (payOrder == null) {
            throw new BadRequestException("支付单不存在：" + id);
        }
        if (!Objects.equals(payOrder.getBizUserId(), UserContext.requireUser())) {
            throw new ForbiddenException("无权查看他人的支付单");
        }
        return toVO(payOrder);
    }

    /** 查询订单并逐项校验，任一项不满足都直接抛业务异常（不会扣款）。 */
    private OrderDTO requirePayableOrder(Long userId, PayOrderFormDTO form) {
        R<OrderDTO> orderResult = tradeClient.queryOrderById(form.bizOrderNo());
        OrderDTO order = orderResult == null ? null : orderResult.data();
        if (order == null) {
            throw new BizIllegalException("订单不存在或不可支付：" + form.bizOrderNo());
        }
        if (!Objects.equals(order.userId(), userId)) {
            // trade-service 内部也会做归属校验，这里再挡一层
            throw new ForbiddenException("无权支付他人的订单");
        }
        if (!Objects.equals(order.status(), ORDER_STATUS_UN_PAID)) {
            throw new BizIllegalException("订单状态不允许支付：" + order.status());
        }
        if (!Objects.equals(order.totalFee(), form.amount())) {
            throw new BizIllegalException("支付金额与订单金额不一致，订单金额：" + order.totalFee() + " 分");
        }
        return order;
    }

    private PayOrderVO toVO(PayOrder payOrder) {
        return new PayOrderVO(payOrder.getId(), payOrder.getBizOrderNo(), payOrder.getAmount(),
                payOrder.getStatus(), payOrder.getPayChannelCode(), payOrder.getPaySuccessTime());
    }
}
