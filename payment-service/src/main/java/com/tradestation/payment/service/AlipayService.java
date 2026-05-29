package com.tradestation.payment.service;

import java.math.BigDecimal;
import java.util.Map;

public interface AlipayService {

    boolean isConfigured();

    /**
     * 生成电脑网站支付 HTML 表单
     */
    String createPagePay(String paymentNo, BigDecimal amount, String subject);

    /**
     * 验证支付宝异步通知签名
     */
    boolean verifyNotify(Map<String, String> params);

    /**
     * 主动查询支付宝支付状态（用于同步回调后的二次确认）
     * @param paymentNo 支付单号（商户订单号 out_trade_no）
     * @return 支付宝返回的原始参数，包含 trade_status 等字段
     */
    Map<String, String> queryPayStatus(String paymentNo);
}
