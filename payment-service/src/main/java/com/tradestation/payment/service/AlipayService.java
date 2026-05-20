package com.tradestation.payment.service;

import java.math.BigDecimal;
import java.util.Map;

public interface AlipayService {

    boolean isConfigured();

    String createPagePay(String paymentNo, BigDecimal amount, String subject);

    boolean verifyNotify(Map<String, String> params);
}
