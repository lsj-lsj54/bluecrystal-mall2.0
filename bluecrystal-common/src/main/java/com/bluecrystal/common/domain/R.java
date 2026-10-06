package com.bluecrystal.common.domain;

/**
 * 统一响应体。
 *
 * <p>使用 record 定义，Jackson 2.21 原生支持序列化；code 约定 200 表示成功。
 */
public record R<T>(int code, String msg, T data) {

    /** 成功且无数据。 */
    public static R<Void> ok() {
        return new R<>(200, "OK", null);
    }

    /** 成功并携带数据。 */
    public static <T> R<T> ok(T data) {
        return new R<>(200, "OK", data);
    }

    /** 业务失败，默认 500。 */
    public static <T> R<T> error(String msg) {
        return new R<>(500, msg, null);
    }

    /** 指定错误码失败。 */
    public static <T> R<T> error(int code, String msg) {
        return new R<>(code, msg, null);
    }

    public boolean success() {
        return code == 200;
    }
}
