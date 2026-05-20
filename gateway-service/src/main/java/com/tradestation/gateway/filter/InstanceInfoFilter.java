package com.tradestation.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebExchangeDecorator;
import reactor.core.publisher.Mono;
import org.reactivestreams.Publisher;

import java.net.URI;

@Component
public class InstanceInfoFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(InstanceInfoFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 用 beforeCommit 确保响应提交前注入头
        exchange.getResponse().beforeCommit(() -> {
            addInstanceHeader(exchange);
            return Mono.empty();
        });

        ServerWebExchangeDecorator decorator = new ServerWebExchangeDecorator(exchange) {
            private ServerHttpResponseDecorator responseDecorator;

            @Override
            public ServerHttpResponseDecorator getResponse() {
                if (responseDecorator == null) {
                    responseDecorator = new ServerHttpResponseDecorator(super.getResponse()) {
                        @Override
                        public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                            addInstanceHeader(exchange);
                            log.warn("writeWith called");
                            return super.writeWith(body);
                        }

                        @Override
                        public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
                            addInstanceHeader(exchange);
                            log.warn("writeAndFlushWith called");
                            return super.writeAndFlushWith(body);
                        }

                        @Override
                        public Mono<Void> setComplete() {
                            addInstanceHeader(exchange);
                            return super.setComplete();
                        }
                    };
                }
                return responseDecorator;
            }
        };

        log.warn("InstanceInfoFilter executing for: {}", exchange.getRequest().getURI());
        return chain.filter(decorator);
    }

    private void addInstanceHeader(ServerWebExchange exchange) {
        URI requestUrl = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
        if (requestUrl != null) {
            String instance = requestUrl.getHost() + ":" + requestUrl.getPort();
            exchange.getResponse().getHeaders().add("X-Instance", instance);
            log.warn("(负载均衡) X-Instance added: {}", instance);
        } else {
            log.warn("(负载均衡) GATEWAY_REQUEST_URL_ATTR is null");
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
