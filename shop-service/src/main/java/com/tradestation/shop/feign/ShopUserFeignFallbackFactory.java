package com.tradestation.shop.feign;

import com.tradestation.common.enums.ErrorCode;
import com.tradestation.common.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ShopUserFeignFallbackFactory implements FallbackFactory<ShopUserFeignClient> {

    private static final Logger log = LoggerFactory.getLogger(ShopUserFeignFallbackFactory.class);

    @Override
    public ShopUserFeignClient create(Throwable cause) {
        log.error("ShopUserFeignClient fallback triggered", cause);
        return new ShopUserFeignClient() {
            @Override
            public Result<Void> updateRole(Long id, Map<String, String> body) {
                log.error("(触发降级)updateRole fallback, id={}", id);
                return Result.fail(ErrorCode.SERVICE_DEGRADED);
            }

            @Override
            public Result<Map<String, Object>> getUserInfo(Long id) {
                log.error("(触发降级)getUserInfo fallback, id={}", id);
                return Result.fail(ErrorCode.SERVICE_DEGRADED);
            }
        };
    }
}