package com.tradestation.common.enums;

public enum ErrorCode {

    SUCCESS(200, "success"),

    BAD_REQUEST(400, "bad request"),
    UNAUTHORIZED(401, "unauthorized"),
    FORBIDDEN(403, "forbidden"),
    NOT_FOUND(404, "resource not found"),
    CONFLICT(409, "conflict"),
    PARAM_ERROR(422, "parameter validation failed"),

    INTERNAL_ERROR(500, "网络异常，请稍后重试"),
    SERVICE_UNAVAILABLE(503, "service unavailable"),

    // Business errors (1xxx: user, 2xxx: shop, 3xxx: product, 4xxx: order, 5xxx: payment)
    USER_NOT_FOUND(1001, "user not found"),
    USER_PASSWORD_ERROR(1002, "password error"),
    USER_TOKEN_INVALID(1003, "token invalid or expired"),
    USER_ALREADY_EXISTS(1004, "user already exists"),

    SHOP_NOT_FOUND(2001, "shop not found"),
    SHOP_STATUS_INVALID(2002, "shop status invalid"),
    SHOP_ALREADY_EXISTS(2003, "shop already exists"),

    PRODUCT_NOT_FOUND(3001, "product not found"),
    STOCK_INSUFFICIENT(3002, "stock insufficient"),
    PRODUCT_OFF_SHELF(3003, "product is off shelf"),

    ORDER_NOT_FOUND(4001, "order not found"),
    ORDER_STATUS_INVALID(4002, "order status invalid for this operation"),

    PAYMENT_NOT_FOUND(5001, "payment not found"),
    PAYMENT_ALREADY_PROCESSED(5002, "payment already processed"),

    // Sentinel fault tolerance
    RATE_LIMITED(429, "(触发限流)请求过于频繁，请稍后重试"),
    SERVICE_DEGRADED(503, "(触发降级)推荐服务暂不可用，正在恢复中"),
    CIRCUIT_BREAKER(503, "(熔断)当前服务拥挤，已自动为您排队，请稍后重试");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() { return code; }
    public String getMessage() { return message; }
}
