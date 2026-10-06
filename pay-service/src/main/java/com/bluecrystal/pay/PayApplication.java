package com.bluecrystal.pay;

import com.bluecrystal.api.config.DefaultFeignConfig;
import com.bluecrystal.api.config.SeataFeignConfig;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@MapperScan("com.bluecrystal.pay.mapper")
@EnableFeignClients(basePackages = "com.bluecrystal.api.client",
        defaultConfiguration = {DefaultFeignConfig.class, SeataFeignConfig.class})
public class PayApplication {

    public static void main(String[] args) {
        SpringApplication.run(PayApplication.class, args);
    }
}
