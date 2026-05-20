package com.tradestation.order.feign;

import com.tradestation.common.enums.ErrorCode;
import com.tradestation.common.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class ProductFeignFallbackFactory implements FallbackFactory<ProductFeignClient> {

    private static final Logger log = LoggerFactory.getLogger(ProductFeignFallbackFactory.class);

    @Override
    public ProductFeignClient create(Throwable cause) {
        log.error("ProductFeignClient fallback triggered", cause);
        return new ProductFeignClient() {
            @Override
            public Result<List<Map<String, Object>>> getSkuBatch(String ids) {
                log.error("(触发降级)getSkuBatch fallback, ids={}", ids);
                return Result.fail(ErrorCode.SERVICE_DEGRADED);
            }

            @Override
            public Result<Void> deductInventory(List<Map<String, Object>> items) {
                log.error("(触发降级)deductInventory fallback");
                return Result.fail(ErrorCode.SERVICE_DEGRADED);
            }

            @Override
            public Result<Void> restoreInventory(List<Map<String, Object>> items) {
                log.error("(触发降级)restoreInventory fallback");
                return Result.fail(ErrorCode.SERVICE_DEGRADED);
            }
        };
    }
}
