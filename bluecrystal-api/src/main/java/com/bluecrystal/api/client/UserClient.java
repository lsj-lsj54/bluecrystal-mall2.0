package com.bluecrystal.api.client;

import com.bluecrystal.common.domain.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 用户服务客户端（内部接口）。
 *
 * <p>路径挂在 {@code /internal/**} 下，网关未配置该前缀的路由，因此不对外暴露；
 * 用户服务内部还会校验被扣款用户必须是当前登录用户本身。
 */
@FeignClient(name = "user-service", path = "/internal/users", contextId = "userClient")
public interface UserClient {

    /** 扣减余额，amount 单位为分；余额不足时由用户服务抛出业务异常。 */
    @PostMapping("/{userId}/balance/deduct")
    R<Void> deductBalance(@PathVariable("userId") Long userId, @RequestParam("amount") Integer amount);
}
