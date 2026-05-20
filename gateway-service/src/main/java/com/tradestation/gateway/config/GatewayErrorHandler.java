package com.tradestation.gateway.config;

import com.tradestation.common.enums.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
@Order(-2)
public class GatewayErrorHandler implements ErrorWebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GatewayErrorHandler.class);

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body;

        if (ex instanceof NotFoundException
                || ex instanceof java.util.concurrent.TimeoutException
                || (ex instanceof ResponseStatusException
                && ((ResponseStatusException) ex).getStatus() == HttpStatus.SERVICE_UNAVAILABLE)) {
            log.error("(触发降级)Downstream service unavailable: {}", ex.getMessage());
        } else {
            log.error("(触发降级)Gateway unexpected error: ", ex);
        }
        body = formatError(ErrorCode.SERVICE_DEGRADED);

        response.setStatusCode(HttpStatus.OK);
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    private String formatError(ErrorCode errorCode) {
        return "{\"code\":" + errorCode.getCode()
                + ",\"message\":\"" + errorCode.getMessage()
                + "\",\"data\":null}";
    }
}
