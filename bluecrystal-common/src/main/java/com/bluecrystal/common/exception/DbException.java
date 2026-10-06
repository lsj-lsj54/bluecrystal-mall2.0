package com.bluecrystal.common.exception;

/** 500：数据库或中间件访问失败。 */
public class DbException extends CommonException {

    public DbException(String message) {
        super(500, message);
    }

    public DbException(String message, Throwable cause) {
        super(500, message, cause);
    }
}
