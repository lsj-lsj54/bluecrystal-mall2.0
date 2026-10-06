package com.bluecrystal.common.exception;

/** 400：违反业务规则（如库存不足、状态不允许）。 */
public class BizIllegalException extends CommonException {

    public BizIllegalException(String message) {
        super(400, message);
    }
}
