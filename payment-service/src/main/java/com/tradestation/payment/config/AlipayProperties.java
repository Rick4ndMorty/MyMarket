package com.tradestation.payment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "alipay")
public class AlipayProperties {
    private String appId;
    private String privateKey;
    private String alipayPublicKey;
    private String gateway;
    private String notifyUrl;
    private String returnUrl;
    private String charset = "UTF-8";
    private String format = "json";
    private String signType = "RSA2";

    public boolean isConfigured() {
        return appId != null && !appId.isBlank()
                && privateKey != null && !privateKey.isBlank()
                && alipayPublicKey != null && !alipayPublicKey.isBlank();
    }
}
