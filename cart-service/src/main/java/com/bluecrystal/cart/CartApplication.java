package com.bluecrystal.cart;

import com.bluecrystal.api.config.DefaultFeignConfig;
import com.bluecrystal.api.config.SeataFeignConfig;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@MapperScan("com.bluecrystal.cart.mapper")
@EnableFeignClients(basePackages = "com.bluecrystal.api.client",
        defaultConfiguration = {DefaultFeignConfig.class, SeataFeignConfig.class})
public class CartApplication {

    public static void main(String[] args) {
        SpringApplication.run(CartApplication.class, args);
    }
}
