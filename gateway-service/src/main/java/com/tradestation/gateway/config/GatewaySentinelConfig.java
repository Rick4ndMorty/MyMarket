package com.tradestation.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.sc.SentinelGatewayFilter;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import com.alibaba.csp.sentinel.adapter.gateway.sc.exception.SentinelGatewayBlockExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;

/**
 * Gateway 层 Sentinel 配置
 * - 自定义限流/熔断时的 JSON 响应拦截
 * - 可按路由配置 QPS 限流规则
 */
@Configuration
public class GatewaySentinelConfig {

    private static final Logger log = LoggerFactory.getLogger(GatewaySentinelConfig.class);

    private final List<ViewResolver> viewResolvers;
    private final ServerCodecConfigurer serverCodecConfigurer;

    public GatewaySentinelConfig(
            ObjectProvider<List<ViewResolver>> viewResolversProvider,
            ServerCodecConfigurer serverCodecConfigurer) {
        this.viewResolvers = viewResolversProvider.getIfAvailable(Collections::emptyList);
        this.serverCodecConfigurer = serverCodecConfigurer;
    }

    /**
     * 自定义异常处理器：返回 JSON 而非默认 HTML
     * 优先级最高以确保我们的处理器被使用
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SentinelGatewayBlockExceptionHandler sentinelGatewayBlockExceptionHandler() {
        return new SentinelGatewayBlockExceptionHandler(viewResolvers, serverCodecConfigurer);
    }

    /**
     * 自定义限流/熔断时的返回内容
     * 当触发流量控制或降级时，返回 429 状态码和自定义 JSON 消息
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public BlockRequestHandler blockRequestHandler() {
        return new BlockRequestHandler() {
            @Override
            public Mono<ServerResponse> handleRequest(ServerWebExchange exchange, Throwable t) {
                log.warn("(触发限流)Gateway Sentinel blocked: path={}, error={}",
                        exchange.getRequest().getPath(), t.getMessage());
                return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue("{\"code\":429,\"message\":\"请求过于频繁，请稍后重试\"}");
            }
        };
    }

    @PostConstruct
    public void initRules() {
        // Gateway 规则通过 Sentinel Dashboard 动态管理更灵活
        // 这里仅做初始化占位，实际规则建议在 Dashboard 控制台配置
        log.info("Gateway Sentinel config initialized, use Dashboard (http://127.0.0.1:8858) to manage rules");
    }
}