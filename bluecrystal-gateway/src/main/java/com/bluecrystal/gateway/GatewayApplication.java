package com.bluecrystal.gateway;

import com.bluecrystal.gateway.config.AuthProperties;
import com.bluecrystal.gateway.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 网关入口：所有外部请求统一从 8080 进入，鉴权后按 Nacos 注册的服务名转发。
 */
@SpringBootApplication
@EnableConfigurationProperties({JwtProperties.class, AuthProperties.class})
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
