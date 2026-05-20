package com.tradestation.order.feign;

import com.tradestation.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(
    name = "product-service",
    fallbackFactory = ProductFeignFallbackFactory.class
)
public interface ProductFeignClient {

    @GetMapping("/product/sku/batch")
    Result<List<Map<String, Object>>> getSkuBatch(@RequestParam("ids") String ids);

    @PutMapping("/product/inventory/deduct")
    Result<Void> deductInventory(@RequestBody List<Map<String, Object>> items);

    @PutMapping("/product/inventory/restore")
    Result<Void> restoreInventory(@RequestBody List<Map<String, Object>> items);
}
