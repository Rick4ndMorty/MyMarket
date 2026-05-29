package com.tradestation.payment.service.impl;

import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
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
            log.info("Alipay config check: appId={}, gateway={}, signType={}",
                    alipayProperties.getAppId(),
                    alipayProperties.getGateway(),
                    alipayProperties.getSignType());
            log.info("Alipay privateKey length={}, alipayPublicKey length={}",
                    alipayProperties.getPrivateKey().length(),
                    alipayProperties.getAlipayPublicKey().length());

            // 本地验证密钥对：用私钥签名测试字符串，用应用公钥验签
            // 注意：这里的 alipay-public-key 是支付宝公钥，不同于应用公钥
            // 此处仅验证私钥本身是否能正常签名（能加载即说明格式正确）
            verifyPrivateKey();

            this.alipayClient = new DefaultAlipayClient(
                    alipayProperties.getGateway(),
                    alipayProperties.getAppId(),
                    alipayProperties.getPrivateKey(),
                    alipayProperties.getFormat(),
                    alipayProperties.getCharset(),
                    alipayProperties.getAlipayPublicKey(),
                    alipayProperties.getSignType()
            );
            log.info("Alipay client initialized successfully");
        } else {
            log.info("Alipay not configured — will fall back to MOCK payment");
        }
    }

    /**
     * 本地验证私钥格式是否可正常加载和签名
     */
    private void verifyPrivateKey() {
        try {
            java.security.spec.PKCS8EncodedKeySpec keySpec =
                    new java.security.spec.PKCS8EncodedKeySpec(
                            java.util.Base64.getDecoder().decode(alipayProperties.getPrivateKey()));
            java.security.KeyFactory kf = java.security.KeyFactory.getInstance("RSA");
            java.security.PrivateKey pk = kf.generatePrivate(keySpec);

            // 签名测试
            java.security.Signature sign = java.security.Signature.getInstance("SHA256withRSA");
            sign.initSign(pk);
            byte[] testData = "hello_alipay".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            sign.update(testData);
            byte[] sig = sign.sign();

            // 如果有应用公钥，验证签名/验签是否匹配
            String appPubKey = alipayProperties.getAppPublicKey();
            if (appPubKey != null && !appPubKey.isBlank()) {
                java.security.spec.X509EncodedKeySpec pubKeySpec =
                        new java.security.spec.X509EncodedKeySpec(
                                java.util.Base64.getDecoder().decode(appPubKey));
                java.security.PublicKey pubKey = kf.generatePublic(pubKeySpec);

                java.security.Signature verify = java.security.Signature.getInstance("SHA256withRSA");
                verify.initVerify(pubKey);
                verify.update(testData);
                boolean match = verify.verify(sig);
                if (match) {
                    log.info("Key pair verification PASSED: private key matches application public key");
                } else {
                    log.error("Key pair verification FAILED: private key does NOT match application public key! "
                            + "You may have uploaded the wrong key pair to Alipay.");
                }
            } else {
                log.info("Private key self-test PASSED: signed successfully, signature bytes={} "
                        + "(app-public-key not configured, skipping pair verification)",
                        sig.length);
            }
        } catch (Exception e) {
            log.error("Private key self-test FAILED: {}", e.getMessage(), e);
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
        // 支付宝要求金额精确到小数点后两位（如 199.00）
        bizContent.put("total_amount", amount.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString());
        bizContent.put("subject", subject);
        bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");
        String bizJson = JSON.toJSONString(bizContent);
        request.setBizContent(bizJson);
        log.info("Alipay bizContent: {}", bizJson);

        try {
            String form = alipayClient.pageExecute(request).getBody();
            // 打印表单前500字符用于调试
            log.info("Alipay page pay form (first 500 chars): {}", 
                    form.length() > 500 ? form.substring(0, 500) : form);
            log.info("Alipay page pay form generated: paymentNo={}, amount={}, subject={}",
                    paymentNo, amount, subject);
            return form;
        } catch (AlipayApiException e) {
            log.error("Failed to create Alipay page pay: paymentNo={}, errCode={}, errMsg={}",
                    paymentNo, e.getErrCode(), e.getErrMsg(), e);
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

    @Override
    public Map<String, String> queryPayStatus(String paymentNo) {
        if (!isConfigured()) {
            throw new IllegalStateException("Alipay is not configured");
        }

        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        Map<String, Object> bizContent = new HashMap<>();
        bizContent.put("out_trade_no", paymentNo);
        request.setBizContent(JSON.toJSONString(bizContent));

        try {
            AlipayTradeQueryResponse response = alipayClient.execute(request);
            log.info("Alipay query result: paymentNo={}, tradeStatus={}, code={}, msg={}",
                    paymentNo, response.getTradeStatus(), response.getCode(), response.getMsg());

            Map<String, String> result = new HashMap<>();
            result.put("trade_status", response.getTradeStatus());
            result.put("trade_no", response.getTradeNo());
            result.put("out_trade_no", response.getOutTradeNo());
            result.put("total_amount", response.getTotalAmount());
            result.put("buyer_logon_id", response.getBuyerLogonId());
            result.put("code", response.getCode());
            result.put("msg", response.getMsg());
            result.put("sub_code", response.getSubCode());
            result.put("sub_msg", response.getSubMsg());
            return result;
        } catch (AlipayApiException e) {
            log.error("Failed to query Alipay payment status: paymentNo={}", paymentNo, e);
            throw new RuntimeException("Alipay query failed: " + e.getMessage(), e);
        }
    }
}
