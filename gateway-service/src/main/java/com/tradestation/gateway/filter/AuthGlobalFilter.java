package com.tradestation.gateway.filter;

import com.tradestation.common.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(AuthGlobalFilter.class);

    private final Tracer tracer;

    public AuthGlobalFilter(Tracer tracer) {
        this.tracer = tracer;
    }

    private static final List<String> WHITELIST = List.of(
            "/api/user/login",
            "/api/user/register",
            "/api/payment/callback/alipay"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        String method = exchange.getRequest().getMethodValue();

        // 白名单放行
        if (isWhitelist(path, method)) {
            ServerHttpRequest.Builder builder = exchange.getRequest().mutate();
            addTraceHeaders(builder);
            return chain.filter(exchange.mutate().request(builder.build()).build());
        }

        // OPTIONS 预检请求放行
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequest().getMethodValue())) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange, "Missing or invalid token");
        }

        String token = authHeader.substring(7);
        try {
            Claims claims = JwtUtil.parseToken(token);
            Long userId = JwtUtil.getUserId(claims);
            String username = JwtUtil.getUsername(claims);
            String role = JwtUtil.getRole(claims);

            // 将用户信息透传到下游微服务
            ServerHttpRequest.Builder builder = exchange.getRequest().mutate()
                    .header("X-User-Id", String.valueOf(userId))
                    .header("X-Username", username)
                    .header("X-User-Role", role);

            // 传播 Sleuth 链路追踪头（确保下游服务在同一个 trace 中）
            addTraceHeaders(builder);

            ServerHttpRequest mutatedRequest = builder.build();
            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (Exception e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return unauthorized(exchange, "Token invalid or expired");
        }
    }

    private void addTraceHeaders(ServerHttpRequest.Builder builder) {
        Span span = tracer.currentSpan();
        if (span != null) {
            String traceId = span.context().traceId();
            String spanId = span.context().spanId();
            if (traceId != null) {
                builder.header("X-B3-TraceId", traceId);
                builder.header("X-B3-SpanId", spanId);
                builder.header("X-B3-Sampled", "1");
            }
        }
    }

    private boolean isWhitelist(String path, String method) {
        // 管理端路径不走白名单，必须认证
        if (path.contains("/admin")) {
            return false;
        }
        // GET /api/product 公开（搜索/详情），POST/PUT 需要认证
        if ("GET".equalsIgnoreCase(method) && path.startsWith("/api/product")) {
            return true;
        }
        for (String w : WHITELIST) {
            if (path.startsWith(w)) {
                return true;
            }
        }
        return false;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().set(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8");
        String body = "{\"code\":401,\"message\":\"" + message + "\",\"data\":null}";
        return response.writeWith(Mono.fromSupplier(() -> {
            byte[] bytes = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            return response.bufferFactory().wrap(bytes);
        }));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
