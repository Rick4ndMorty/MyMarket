package com.tradestation.common.feign;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.MDC;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

public class FeignAuthInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String token = request.getHeader("Authorization");
            if (token != null) {
                template.header("Authorization", token);
            }
        }

        // 传递 Sleuth traceId
        String traceId = MDC.get("traceId");
        if (traceId != null) {
            template.header("X-B3-TraceId", traceId);
        }
        String spanId = MDC.get("spanId");
        if (spanId != null) {
            template.header("X-B3-SpanId", spanId);
        }
    }
}
