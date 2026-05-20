package com.tradestation.order.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单事件消息体 — 通过 Spring Cloud Stream 发送到 RabbitMQ
 */
public class OrderEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long orderId;
    private String orderNo;
    private Long userId;
    private Long shopId;
    private BigDecimal totalAmount;
    private String eventType;
    private LocalDateTime timestamp;

    public OrderEvent() {}

    public OrderEvent(Long orderId, String orderNo, Long userId, Long shopId,
                      BigDecimal totalAmount, String eventType) {
        this.orderId = orderId;
        this.orderNo = orderNo;
        this.userId = userId;
        this.shopId = shopId;
        this.totalAmount = totalAmount;
        this.eventType = eventType;
        this.timestamp = LocalDateTime.now();
    }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return "OrderEvent{orderId=" + orderId + ", orderNo='" + orderNo
                + "', userId=" + userId + ", shopId=" + shopId
                + ", totalAmount=" + totalAmount + ", eventType='" + eventType + "'}";
    }
}