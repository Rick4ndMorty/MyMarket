package com.tradestation.order.feign;

import com.tradestation.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(
    name = "user-service",
    fallbackFactory = UserFeignFallbackFactory.class
)
public interface UserFeignClient {

    @GetMapping("/user/address/{id}")
    Result<Map<String, Object>> getAddress(@PathVariable("id") Long id);

    @GetMapping("/user/inner/{id}")
    Result<Map<String, Object>> getUserById(@PathVariable("id") Long id);
}
