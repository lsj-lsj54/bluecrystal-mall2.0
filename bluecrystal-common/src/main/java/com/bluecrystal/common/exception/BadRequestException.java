package com.bluecrystal.common.exception;

/** 400：请求参数或业务前置条件不合法。 */
public class BadRequestException extends CommonException {

    public BadRequestException(String message) {
        super(400, message);
    }
}
