package com.tradestation.shop.feign;

import com.tradestation.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(
    name = "user-service",
    fallbackFactory = ShopUserFeignFallbackFactory.class
)
public interface ShopUserFeignClient {

    @PutMapping("/user/{id}/role")
    Result<Void> updateRole(@PathVariable("id") Long id, @RequestBody Map<String, String> body);

    @GetMapping("/user/inner/{id}")
    Result<Map<String, Object>> getUserInfo(@PathVariable("id") Long id);
}
