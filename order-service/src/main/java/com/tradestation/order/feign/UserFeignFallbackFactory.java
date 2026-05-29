package com.tradestation.order.feign;

import com.tradestation.common.enums.ErrorCode;
import com.tradestation.common.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class UserFeignFallbackFactory implements FallbackFactory<UserFeignClient> {

    private static final Logger log = LoggerFactory.getLogger(UserFeignFallbackFactory.class);

    @Override
    public UserFeignClient create(Throwable cause) {
        log.error("UserFeignClient fallback triggered", cause);
        return new UserFeignClient() {
            @Override
            public Result<Map<String, Object>> getAddress(Long id) {
                log.error("(触发降级)getAddress fallback, addressId={}", id);
                return Result.fail(ErrorCode.SERVICE_DEGRADED);
            }

            @Override
            public Result<Map<String, Object>> getUserById(Long id) {
                log.error("(触发降级)getUserById fallback, userId={}", id);
                return Result.fail(ErrorCode.SERVICE_DEGRADED);
            }
        };
    }
}
