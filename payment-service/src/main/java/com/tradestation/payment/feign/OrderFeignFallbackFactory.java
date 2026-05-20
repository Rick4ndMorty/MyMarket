package com.tradestation.payment.feign;

import com.tradestation.common.enums.ErrorCode;
import com.tradestation.common.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class OrderFeignFallbackFactory implements FallbackFactory<OrderFeignClient> {

    private static final Logger log = LoggerFactory.getLogger(OrderFeignFallbackFactory.class);

    @Override
    public OrderFeignClient create(Throwable cause) {
        log.error("OrderFeignClient fallback triggered", cause);
        return new OrderFeignClient() {
            @Override
            public Result<java.util.Map<String, Object>> getOrder(Long id) {
                log.error("(触发降级)getOrder fallback, orderId={}", id);
                return Result.fail(ErrorCode.SERVICE_DEGRADED);
            }

            @Override
            public Result<Void> updateStatus(Long id, java.util.Map<String, Object> body) {
                log.error("(触发降级)updateStatus fallback, orderId={}", id);
                return Result.fail(ErrorCode.SERVICE_DEGRADED);
            }
        };
    }
}
