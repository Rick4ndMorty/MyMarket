package com.tradestation.payment.controller;

import com.tradestation.common.result.Result;
import com.tradestation.payment.dto.CreatePaymentReq;
import com.tradestation.payment.dto.PaymentVO;
import com.tradestation.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public Result<PaymentVO> createPayment(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody CreatePaymentReq req) {
        log.info("create payment: userId={}, orderId={}, method={}", userId, req.getOrderId(), req.getPaymentMethod());
        return paymentService.createPayment(userId, req.getOrderId(), req.getPaymentMethod());
    }

    @GetMapping("/{id}")
    public Result<PaymentVO> getPayment(@PathVariable Long id) {
        return paymentService.getById(id);
    }

    @GetMapping("/no/{paymentNo}")
    public Result<PaymentVO> getPaymentByNo(@PathVariable String paymentNo) {
        return paymentService.getByPaymentNo(paymentNo);
    }

    @PostMapping("/callback/mock/{paymentNo}")
    public Result<Void> mockPayCallback(@PathVariable String paymentNo) {
        log.info("mock payment callback: paymentNo={}", paymentNo);
        return paymentService.mockPayCallback(paymentNo);
    }

    @PostMapping("/callback/alipay")
    public String alipayNotify(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((k, v) -> params.put(k, v[0]));
        log.info("Alipay notify received: paymentNo={}, tradeStatus={}",
                params.get("out_trade_no"), params.get("trade_status"));
        try {
            paymentService.handleAlipayCallback(params);
            return "success";
        } catch (Exception e) {
            log.error("Alipay callback processing failed", e);
            return "failure";
        }
    }

    /**
     * 主动查询支付宝支付状态并同步（用于同步回调后的二次确认）
     * 安全关键：防止用户支付成功但支付宝异步通知未到达的情况
     */
    @PostMapping("/query-alipay/{paymentNo}")
    public Result<PaymentVO> queryAlipayStatus(@PathVariable String paymentNo) {
        log.info("query alipay status: paymentNo={}", paymentNo);
        return paymentService.queryAlipayAndSync(paymentNo);
    }

    @GetMapping("/callback/alipay/return")
    public Result<PaymentVO> alipayReturn(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((k, v) -> params.put(k, v[0]));
        log.info("Alipay return: paymentNo={}", params.get("out_trade_no"));
        String paymentNo = params.get("out_trade_no");
        if (paymentNo != null) {
            return paymentService.getByPaymentNo(paymentNo);
        }
        return Result.fail(com.tradestation.common.enums.ErrorCode.BAD_REQUEST, "missing out_trade_no");
    }
}
