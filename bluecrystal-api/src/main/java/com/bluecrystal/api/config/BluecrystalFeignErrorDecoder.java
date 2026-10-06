package com.bluecrystal.api.config;

import com.bluecrystal.common.domain.R;
import com.bluecrystal.common.exception.CommonException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.Util;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Feign 错误解码器：把下游返回的 HTTP 非 2xx 还原成业务异常。
 *
 * <p>默认的 {@link ErrorDecoder.Default} 只会抛 {@code FeignException}，
 * 调用方拿不到下游 {@link R} 里的 code/msg，最终被全局异常处理兜成「服务器繁忙」，
 * 于是「库存不足」「余额不足」这类真实原因全部丢失。这里把响应体解析回 {@link CommonException}。
 */
@Slf4j
public class BluecrystalFeignErrorDecoder implements ErrorDecoder {

    private final ObjectMapper objectMapper;

    private final ErrorDecoder defaultDecoder = new ErrorDecoder.Default();

    public BluecrystalFeignErrorDecoder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Exception decode(String methodKey, Response response) {
        String body = readBody(response);
        if (body != null && !body.isBlank()) {
            try {
                R<?> result = objectMapper.readValue(body, R.class);
                if (result != null && !result.success() && result.msg() != null) {
                    log.warn("下游返回业务失败：method={}, status={}, code={}, msg={}",
                            methodKey, response.status(), result.code(), result.msg());
                    return new CommonException(result.code(), result.msg());
                }
            } catch (Exception e) {
                // 响应体不是统一响应体（例如网关/容器返回的错误页），走默认处理
                log.debug("下游错误响应体不是统一格式：method={}, status={}", methodKey, response.status());
            }
        }
        if (response.status() == 404) {
            return new CommonException(404, "下游接口不存在：" + methodKey);
        }
        return defaultDecoder.decode(methodKey, response);
    }

    private String readBody(Response response) {
        if (response.body() == null) {
            return null;
        }
        try {
            return Util.toString(response.body().asReader(StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.warn("读取下游错误响应体失败：{}", e.getMessage());
            return null;
        }
    }
}
