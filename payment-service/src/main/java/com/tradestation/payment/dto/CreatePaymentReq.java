package com.tradestation.payment.dto;

public class CreatePaymentReq {

    private Long orderId;
    private String paymentMethod = "MOCK";

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
}
