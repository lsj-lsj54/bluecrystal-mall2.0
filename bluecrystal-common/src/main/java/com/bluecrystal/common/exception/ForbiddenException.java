package com.bluecrystal.common.exception;

/** 403：已登录但无权限。 */
public class ForbiddenException extends CommonException {

    public ForbiddenException(String message) {
        super(403, message);
    }
}
