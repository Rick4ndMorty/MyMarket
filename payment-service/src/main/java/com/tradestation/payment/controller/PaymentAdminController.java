package com.tradestation.payment.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tradestation.common.enums.ErrorCode;
import com.tradestation.common.exception.BusinessException;
import com.tradestation.common.result.Result;
import com.tradestation.payment.entity.PaymentRecord;
import com.tradestation.payment.mapper.PaymentRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/payment/admin")
@RequiredArgsConstructor
public class PaymentAdminController {

    private final PaymentRecordMapper paymentRecordMapper;

    private void checkAdmin(String role) {
        if (!"ADMIN".equals(role)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats(@RequestHeader("X-User-Role") String role) {
        checkAdmin(role);
        Map<String, Object> result = new HashMap<>();
        java.util.List<PaymentRecord> all = paymentRecordMapper.selectList(null);
        result.put("totalPayments", all.size());
        Map<String, Long> byStatus = new HashMap<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (PaymentRecord p : all) {
            byStatus.merge(p.getStatus(), 1L, Long::sum);
            if ("SUCCESS".equals(p.getStatus()) && p.getAmount() != null) {
                totalAmount = totalAmount.add(p.getAmount());
            }
        }
        result.put("byStatus", byStatus);
        result.put("totalSuccessAmount", totalAmount.doubleValue());
        return Result.ok(result);
    }

    @GetMapping("/payments")
    public Result<Page<PaymentRecord>> listPayments(
            @RequestHeader("X-User-Role") String role,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String paymentNo,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String status) {
        checkAdmin(role);
        LambdaQueryWrapper<PaymentRecord> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(paymentNo)) {
            wrapper.like(PaymentRecord::getPaymentNo, paymentNo);
        }
        if (userId != null) {
            wrapper.eq(PaymentRecord::getUserId, userId);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(PaymentRecord::getStatus, status);
        }
        wrapper.orderByDesc(PaymentRecord::getCreateTime);
        return Result.ok(paymentRecordMapper.selectPage(new Page<>(page, size), wrapper));
    }

    // 退款操作
    @PutMapping("/payments/{id}/refund")
    public Result<Void> refundPayment(
            @RequestHeader("X-User-Role") String role,
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        checkAdmin(role);
        PaymentRecord record = paymentRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(ErrorCode.PAYMENT_NOT_FOUND);
        }
        if (!"SUCCESS".equals(record.getStatus())) {
            throw new BusinessException(ErrorCode.PAYMENT_ALREADY_PROCESSED, "只有支付成功的记录才能退款");
        }
        record.setStatus("REFUNDED");
        record.setUpdateTime(java.time.LocalDateTime.now());
        paymentRecordMapper.updateById(record);
        log.info("Admin refunded payment: id={}, paymentNo={}", id, record.getPaymentNo());
        return Result.ok();
    }
}
