package com.bluecrystal.common.exception;

import lombok.Getter;

/**
 * 业务异常基类：携带 HTTP 语义错误码，由 CommonExceptionAdvice 统一转换成响应体。
 */
@Getter
public class CommonException extends RuntimeException {

    private final int code;

    public CommonException(String message) {
        this(500, message);
    }

    public CommonException(int code, String message) {
        super(message);
        this.code = code;
    }

    public CommonException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}
