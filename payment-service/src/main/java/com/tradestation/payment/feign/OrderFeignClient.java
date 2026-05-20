package com.tradestation.payment.feign;

import com.tradestation.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(
    name = "order-service",
    fallbackFactory = OrderFeignFallbackFactory.class
)
public interface OrderFeignClient {

    @GetMapping("/order/internal/{id}")
    Result<Map<String, Object>> getOrder(@PathVariable("id") Long id);

    @PutMapping("/order/internal/{id}/status")
    Result<Void> updateStatus(@PathVariable("id") Long id, @RequestBody Map<String, Object> body);
}
