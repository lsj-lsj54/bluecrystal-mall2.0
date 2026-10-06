package com.bluecrystal.api.config;

import com.bluecrystal.common.exception.CommonException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * 回归测试：下游返回非 2xx 时，必须把统一响应体里的 code/msg 还原成业务异常，
 * 否则「库存不足」「余额不足」这类真实原因会被默认解码器吞成 FeignException。
 */
class BluecrystalFeignErrorDecoderTest {

    private final BluecrystalFeignErrorDecoder decoder =
            new BluecrystalFeignErrorDecoder(new ObjectMapper());

    @Test
    @DisplayName("下游 R 响应体被还原成 CommonException（保留 code 与 msg）")
    void shouldDecodeBusinessError() {
        String body = "{\"code\":400,\"msg\":\"商品库存不足：1\",\"data\":null}";

        Exception exception = decoder.decode("ItemClient#deductStock(List)", response(400, body));

        CommonException common = assertInstanceOf(CommonException.class, exception);
        assertEquals(400, common.getCode());
        assertEquals("商品库存不足：1", common.getMessage());
    }

    @Test
    @DisplayName("响应体不是统一格式时退回默认解码器")
    void shouldFallbackForNonBusinessBody() {
        Exception exception = decoder.decode("ItemClient#queryItemById(Long)",
                response(502, "<html>Bad Gateway</html>"));

        assertInstanceOf(feign.FeignException.class, exception);
    }

    private Response response(int status, String body) {
        Request request = Request.create(Request.HttpMethod.GET, "/items/1", Map.of(),
                null, StandardCharsets.UTF_8, null);
        return Response.builder()
                .status(status)
                .reason("test")
                .request(request)
                .headers(Map.of("Content-Type", java.util.List.of("application/json")))
                .body(body, StandardCharsets.UTF_8)
                .build();
    }
}
