package com.bluecrystal.pay.enums;

/**
 * 支付单状态。
 *
 * <p>取值与 {@code pay_order.status} 字段一一对应。
 */
public enum PayStatus {

    /** 待支付 */
    WAIT_PAY(0),
    /** 已支付 */
    SUCCESS(1),
    /** 已关闭 */
    CLOSED(2);

    private final int value;

    PayStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
