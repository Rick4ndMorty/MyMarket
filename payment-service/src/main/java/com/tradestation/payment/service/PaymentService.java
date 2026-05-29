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

    /**
     * 主动查询支付宝支付状态并同步更新本地记录
     * 用于同步回调（return_url）后的二次确认，防止用户支付成功但异步通知未到达
     */
    Result<PaymentVO> queryAlipayAndSync(String paymentNo);
}
