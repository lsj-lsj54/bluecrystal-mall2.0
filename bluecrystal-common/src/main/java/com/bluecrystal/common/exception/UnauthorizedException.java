package com.bluecrystal.common.exception;

/** 401：未登录或凭证无效。 */
public class UnauthorizedException extends CommonException {

    public UnauthorizedException(String message) {
        super(401, message);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(401, message, cause);
    }
}
