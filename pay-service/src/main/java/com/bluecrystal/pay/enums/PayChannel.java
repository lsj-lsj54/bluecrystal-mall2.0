package com.bluecrystal.pay.enums;

/**
 * 支付渠道。
 *
 * <p>骨架只实现余额支付，支付宝/微信等渠道后续在此扩展。
 */
public enum PayChannel {

    /** 余额支付 */
    BALANCE("balance", "余额支付");

    private final String code;

    private final String desc;

    PayChannel(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
