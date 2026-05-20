package com.tradestation.shop;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {"com.tradestation.shop", "com.tradestation.common"})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.tradestation.shop.feign")
@MapperScan("com.tradestation.shop.mapper")
public class ShopServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShopServiceApplication.class, args);
    }
}
