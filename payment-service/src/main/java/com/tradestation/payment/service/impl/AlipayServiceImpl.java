package com.tradestation.payment.service.impl;

import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.tradestation.payment.config.AlipayProperties;
import com.tradestation.payment.service.AlipayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class AlipayServiceImpl implements AlipayService {

    private final AlipayProperties alipayProperties;
    private AlipayClient alipayClient;

    public AlipayServiceImpl(AlipayProperties alipayProperties) {
        this.alipayProperties = alipayProperties;
    }

    @PostConstruct
    public void init() {
        if (alipayProperties.isConfigured()) {
            this.alipayClient = new DefaultAlipayClient(
                    alipayProperties.getGateway(),
                    alipayProperties.getAppId(),
                    alipayProperties.getPrivateKey(),
                    alipayProperties.getFormat(),
                    alipayProperties.getCharset(),
                    alipayProperties.getAlipayPublicKey(),
                    alipayProperties.getSignType()
            );
            log.info("Alipay client initialized with gateway: {}", alipayProperties.getGateway());
        } else {
            log.info("Alipay not configured — will fall back to MOCK payment");
        }
    }

    @Override
    public boolean isConfigured() {
        return alipayClient != null && alipayProperties.isConfigured();
    }

    @Override
    public String createPagePay(String paymentNo, BigDecimal amount, String subject) {
        if (!isConfigured()) {
            throw new IllegalStateException("Alipay is not configured");
        }

        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        request.setNotifyUrl(alipayProperties.getNotifyUrl());
        request.setReturnUrl(alipayProperties.getReturnUrl());

        Map<String, Object> bizContent = new HashMap<>();
        bizContent.put("out_trade_no", paymentNo);
        bizContent.put("total_amount", amount.toPlainString());
        bizContent.put("subject", subject);
        bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");
        request.setBizContent(JSON.toJSONString(bizContent));

        try {
            String form = alipayClient.pageExecute(request).getBody();
            log.info("Alipay page pay form generated: paymentNo={}, amount={}", paymentNo, amount);
            return form;
        } catch (AlipayApiException e) {
            log.error("Failed to create Alipay page pay: paymentNo={}", paymentNo, e);
            throw new RuntimeException("Alipay page pay creation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean verifyNotify(Map<String, String> params) {
        if (!isConfigured()) {
            return false;
        }
        try {
            boolean verified = AlipaySignature.rsaCheckV1(
                    params,
                    alipayProperties.getAlipayPublicKey(),
                    alipayProperties.getCharset(),
                    alipayProperties.getSignType()
            );
            log.info("Alipay notify verification: {}", verified);
            return verified;
        } catch (AlipayApiException e) {
            log.error("Alipay notify verification failed", e);
            return false;
        }
    }
}
