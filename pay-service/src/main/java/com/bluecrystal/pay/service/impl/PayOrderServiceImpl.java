package com.bluecrystal.pay.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.bluecrystal.api.client.TradeClient;
import com.bluecrystal.api.client.UserClient;
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

    private final PayOrderMapper payOrderMapper;

    private final UserClient userClient;

    private final TradeClient tradeClient;

    @Override
    @GlobalTransactional(name = "apply-pay", rollbackFor = Exception.class)
    @Transactional(rollbackFor = Exception.class)
    public PayOrderVO applyPay(PayOrderFormDTO form) {
        Long userId = UserContext.requireUser();
        if (form.bizOrderNo() == null) {
            throw new BadRequestException("业务订单号不能为空");
        }
        // 金额必须为正；真实场景还应回查 trade-service 的订单金额做核对
        if (form.amount() == null || form.amount() <= 0) {
            throw new BadRequestException("支付金额必须大于 0");
        }

        // TODO 后续接入支付宝/微信：按 payChannelCode 分发到各自的渠道策略实现
        //  （下单预支付、异步回调验签、主动查单），当前骨架只支持余额支付
        if (!PayChannel.BALANCE.getCode().equals(form.payChannelCode())) {
            throw new BizIllegalException("暂不支持的支付渠道");
        }

        // 1. 先落一条待支付的支付单
        PayOrder payOrder = new PayOrder();
        payOrder.setBizOrderNo(form.bizOrderNo());
        payOrder.setBizUserId(userId);
        payOrder.setPayChannelCode(PayChannel.BALANCE.getCode());
        payOrder.setAmount(form.amount());
        payOrder.setStatus(PayStatus.WAIT_PAY.getValue());
        payOrder.setCreateTime(LocalDateTime.now());
        payOrder.setUpdateTime(LocalDateTime.now());
        // 支付单金额必须与表单金额一致，避免渠道接入后出现金额篡改
        if (!Objects.equals(form.amount(), payOrder.getAmount())) {
            throw new BizIllegalException("支付金额与业务订单金额不一致");
        }
        payOrderMapper.insert(payOrder);
        log.info("支付单已创建：id={}, bizOrderNo={}, amount={} 分",
                payOrder.getId(), payOrder.getBizOrderNo(), payOrder.getAmount());

        // 2. 余额支付：扣余额（user-service 分支），返回失败即抛异常，让全局事务回滚
        R<Void> deductResult = userClient.deductBalance(userId, form.amount());
        if (deductResult == null || !deductResult.success()) {
            throw new BizIllegalException(deductResult == null ? "余额支付失败" : deductResult.msg());
        }

        // 3. 回写订单已支付（trade-service 分支）
        R<Void> paidResult = tradeClient.markOrderPaid(form.bizOrderNo());
        if (paidResult == null || !paidResult.success()) {
            throw new BizIllegalException(paidResult == null ? "回写订单支付状态失败" : paidResult.msg());
        }

        // 4. 更新支付单为已支付，并写入支付单号与支付成功时间
        PayOrder success = new PayOrder();
        success.setId(payOrder.getId());
        success.setPayOrderNo(IdWorker.getId());
        success.setStatus(PayStatus.SUCCESS.getValue());
        success.setResultCode("SUCCESS");
        success.setResultMsg("余额支付成功");
        success.setPaySuccessTime(LocalDateTime.now());
        success.setUpdateTime(LocalDateTime.now());
        payOrderMapper.updateById(success);

        log.info("余额支付成功：payOrderNo={}, bizOrderNo={}", success.getPayOrderNo(), form.bizOrderNo());
        return toVO(payOrderMapper.selectById(payOrder.getId()));
    }

    @Override
    public PayOrderVO queryById(Long id) {
        PayOrder payOrder = payOrderMapper.selectById(id);
        if (payOrder == null) {
            throw new BadRequestException("支付单不存在：" + id);
        }
        if (!Objects.equals(payOrder.getBizUserId(), UserContext.requireUser())) {
            throw new ForbiddenException("无权查看他人的支付单");
        }
        return toVO(payOrder);
    }

    private PayOrderVO toVO(PayOrder payOrder) {
        return new PayOrderVO(payOrder.getId(), payOrder.getBizOrderNo(), payOrder.getAmount(),
                payOrder.getStatus(), payOrder.getPayChannelCode(), payOrder.getPaySuccessTime());
    }
}
