package com.tradestation.payment.service;

import com.tradestation.common.result.Result;
import com.tradestation.payment.dto.PaymentVO;

import java.util.Map;

public interface PaymentService {

    Result<PaymentVO> createPayment(Long userId, Long orderId, String paymentMethod);

    Result<PaymentVO> getByPaymentNo(String paymentNo);

    Result<PaymentVO> getById(Long id);

    Result<Void> mockPayCallback(String paymentNo);

    Result<Void> handleAlipayCallback(Map<String, String> params);
}
