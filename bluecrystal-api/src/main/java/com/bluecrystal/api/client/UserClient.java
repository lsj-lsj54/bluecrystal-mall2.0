package com.bluecrystal.api.client;

import com.bluecrystal.common.domain.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 用户服务客户端：支付时扣减余额。
 */
@FeignClient(name = "user-service", path = "/users", contextId = "userClient")
public interface UserClient {

    /** 扣减余额，amount 单位为分；余额不足时由用户服务抛出业务异常。 */
    @PostMapping("/balance/deduct")
    R<Void> deductBalance(@RequestParam("userId") Long userId, @RequestParam("amount") Integer amount);
}
