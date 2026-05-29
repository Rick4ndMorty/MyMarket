package com.tradestation.payment.service.impl;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tradestation.common.enums.ErrorCode;
import com.tradestation.common.exception.BusinessException;
import com.tradestation.common.result.Result;
import com.tradestation.payment.dto.PaymentVO;
import com.tradestation.payment.entity.PaymentRecord;
import com.tradestation.payment.feign.OrderFeignClient;
import com.tradestation.payment.mapper.PaymentRecordMapper;
import com.tradestation.payment.service.AlipayService;
import com.tradestation.payment.service.PaymentService;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRecordMapper paymentRecordMapper;
    private final OrderFeignClient orderFeignClient;
    private final AlipayService alipayService;

    public PaymentServiceImpl(PaymentRecordMapper paymentRecordMapper,
                              OrderFeignClient orderFeignClient,
                              AlipayService alipayService) {
        this.paymentRecordMapper = paymentRecordMapper;
        this.orderFeignClient = orderFeignClient;
        this.alipayService = alipayService;
    }

    @Override
    @SentinelResource(value = "createPayment", blockHandler = "createPaymentBlockHandler", fallback = "createPaymentFallback")
    @Transactional
    public Result<PaymentVO> createPayment(Long userId, Long orderId, String paymentMethod) {
        // 1. 调用订单服务获取订单信息
        Result<Map<String, Object>> orderResult = orderFeignClient.getOrder(orderId);
        if (orderResult.getCode() != 200 || orderResult.getData() == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        Map<String, Object> orderData = orderResult.getData();

        // 2. 校验订单状态
        String orderStatus = String.valueOf(orderData.get("status"));
        if (!"PENDING_PAYMENT".equals(orderStatus)) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "order status is not PENDING_PAYMENT");
        }

        // 3. 校验订单归属
        Object orderUserIdObj = orderData.get("userId");
        Long orderUserId = orderUserIdObj instanceof Number
                ? ((Number) orderUserIdObj).longValue()
                : Long.parseLong(String.valueOf(orderUserIdObj));
        if (!orderUserId.equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "order does not belong to this user");
        }

        // 4. 生成支付单号
        String paymentNo = generatePaymentNo();

        // 5. 提取金额
        Object amountObj = orderData.get("totalAmount");
        BigDecimal amount;
        if (amountObj instanceof BigDecimal) {
            amount = (BigDecimal) amountObj;
        } else if (amountObj instanceof Number) {
            amount = BigDecimal.valueOf(((Number) amountObj).doubleValue());
        } else {
            amount = new BigDecimal(String.valueOf(amountObj));
        }

        if (paymentMethod == null || paymentMethod.isBlank()) {
            paymentMethod = "MOCK";
        }

        // 6. 保存支付记录
        PaymentRecord record = new PaymentRecord();
        record.setPaymentNo(paymentNo);
        record.setOrderId(orderId);
        record.setOrderNo(String.valueOf(orderData.get("orderNo")));
        record.setUserId(userId);
        record.setAmount(amount);
        record.setPaymentMethod(paymentMethod);
        record.setStatus("PENDING");
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());
        paymentRecordMapper.insert(record);

        log.info("payment created: paymentNo={}, orderId={}, amount={}", paymentNo, orderId, amount);

        // 7. 返回 PaymentVO (Alipay 时附加跳转 URL)
        PaymentVO vo = toVO(record);
        if ("ALIPAY".equals(paymentMethod) && alipayService.isConfigured()) {
            String subject = "订单" + String.valueOf(orderData.get("orderNo"));
            try {
                String payUrl = alipayService.createPagePay(paymentNo, amount, subject);
                vo.setPayUrl(payUrl);
            } catch (Exception e) {
                log.error("Alipay page pay failed, fallback to MOCK: paymentNo={}", paymentNo, e);
            }
        }
        return Result.ok(vo);
    }

    public Result<PaymentVO> createPaymentBlockHandler(Long userId, Long orderId, String paymentMethod, BlockException e) {
        log.error("(触发限流)createPayment blocked by Sentinel: userId={}, orderId={}", userId, orderId, e);
        return Result.fail(ErrorCode.RATE_LIMITED);
    }

    public Result<PaymentVO> createPaymentFallback(Long userId, Long orderId, String paymentMethod, Throwable e) {
        log.error("(熔断)createPayment fallback: userId={}, orderId={}, error={}", userId, orderId, e.getMessage());
        return Result.fail(ErrorCode.CIRCUIT_BREAKER);
    }

    @Override
    public Result<PaymentVO> getByPaymentNo(String paymentNo) {
        PaymentRecord record = paymentRecordMapper.selectOne(
                new LambdaQueryWrapper<PaymentRecord>()
                        .eq(PaymentRecord::getPaymentNo, paymentNo));
        if (record == null) {
            throw new BusinessException(ErrorCode.PAYMENT_NOT_FOUND);
        }
        return Result.ok(toVO(record));
    }

    @Override
    public Result<PaymentVO> getById(Long id) {
        PaymentRecord record = paymentRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(ErrorCode.PAYMENT_NOT_FOUND);
        }
        return Result.ok(toVO(record));
    }

    @Override
    @GlobalTransactional(name = "mockPayCallback", rollbackFor = Exception.class)
    @SentinelResource(value = "mockPayCallback", blockHandler = "mockPayCallbackBlockHandler", fallback = "mockPayCallbackFallback")
    @Transactional
    public Result<Void> mockPayCallback(String paymentNo) {
        // 1. 查找支付记录
        PaymentRecord record = paymentRecordMapper.selectOne(
                new LambdaQueryWrapper<PaymentRecord>()
                        .eq(PaymentRecord::getPaymentNo, paymentNo));
        if (record == null) {
            throw new BusinessException(ErrorCode.PAYMENT_NOT_FOUND);
        }

        // 2. 校验状态，防止重复处理
        if (!"PENDING".equals(record.getStatus())) {
            throw new BusinessException(ErrorCode.PAYMENT_ALREADY_PROCESSED);
        }

        // 3. 更新支付记录为成功
        record.setStatus("SUCCESS");
        record.setPayTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());
        paymentRecordMapper.updateById(record);

        // 4. 通知订单服务更新状态
        Map<String, Object> statusBody = Map.of("status", "PENDING_SHIP");
        Result<Void> updateResult = orderFeignClient.updateStatus(record.getOrderId(), statusBody);
        if (updateResult.getCode() != 200) {
            log.warn("failed to update order status after payment: paymentNo={}, orderId={}",
                    paymentNo, record.getOrderId());
        }

        log.info("mock payment callback processed: paymentNo={}, orderId={}", paymentNo, record.getOrderId());
        return Result.ok();
    }

    public Result<Void> mockPayCallbackBlockHandler(String paymentNo, BlockException e) {
        log.error("(触发限流)mockPayCallback blocked by Sentinel: paymentNo={}", paymentNo, e);
        return Result.fail(ErrorCode.RATE_LIMITED);
    }

    public Result<Void> mockPayCallbackFallback(String paymentNo, Throwable e) {
        log.error("(熔断)mockPayCallback fallback: paymentNo={}, error={}", paymentNo, e.getMessage());
        return Result.fail(ErrorCode.CIRCUIT_BREAKER);
    }

    @Override
    @GlobalTransactional(name = "handleAlipayCallback", rollbackFor = Exception.class)
    @SentinelResource(value = "handleAlipayCallback", blockHandler = "handleAlipayCallbackBlockHandler", fallback = "handleAlipayCallbackFallback")
    @Transactional
    public Result<Void> handleAlipayCallback(Map<String, String> params) {
        if (!alipayService.isConfigured()) {
            return Result.fail(ErrorCode.SERVICE_UNAVAILABLE, "Alipay not configured");
        }

        if (!alipayService.verifyNotify(params)) {
            log.warn("Alipay notify signature verification failed");
            return Result.fail(ErrorCode.BAD_REQUEST, "signature verification failed");
        }

        String paymentNo = params.get("out_trade_no");
        String tradeStatus = params.get("trade_status");

        if (!"TRADE_SUCCESS".equals(tradeStatus)) {
            log.info("Alipay notify: trade status is {}, skip processing", tradeStatus);
            return Result.ok();
        }

        PaymentRecord record = paymentRecordMapper.selectOne(
                new LambdaQueryWrapper<PaymentRecord>()
                        .eq(PaymentRecord::getPaymentNo, paymentNo));
        if (record == null) {
            throw new BusinessException(ErrorCode.PAYMENT_NOT_FOUND);
        }

        if (!"PENDING".equals(record.getStatus())) {
            log.info("Payment already processed: paymentNo={}, status={}", paymentNo, record.getStatus());
            return Result.ok();
        }

        record.setStatus("SUCCESS");
        record.setPayTime(LocalDateTime.now());
        record.setCallbackData(JSON.toJSONString(params));
        record.setUpdateTime(LocalDateTime.now());
        paymentRecordMapper.updateById(record);

        Map<String, Object> statusBody = Map.of("status", "PENDING_SHIP");
        orderFeignClient.updateStatus(record.getOrderId(), statusBody);

        log.info("Alipay callback processed: paymentNo={}, orderId={}", paymentNo, record.getOrderId());
        return Result.ok();
    }

    public Result<Void> handleAlipayCallbackBlockHandler(Map<String, String> params, BlockException e) {
        log.error("(触发限流)handleAlipayCallback blocked by Sentinel", e);
        return Result.fail(ErrorCode.RATE_LIMITED);
    }

    public Result<Void> handleAlipayCallbackFallback(Map<String, String> params, Throwable e) {
        log.error("(熔断)handleAlipayCallback fallback: error={}", e.getMessage());
        return Result.fail(ErrorCode.CIRCUIT_BREAKER);
    }

    /**
     * 主动查询支付宝支付状态并同步更新本地记录
     * 安全关键：即使异步通知未到达，通过此方法也能确保支付状态正确同步
     * 实现幂等性：已支付成功的订单不会被重复处理
     */
    @Override
    @GlobalTransactional(name = "queryAlipayAndSync", rollbackFor = Exception.class)
    @Transactional
    public Result<PaymentVO> queryAlipayAndSync(String paymentNo) {
        // 1. 查找本地支付记录
        PaymentRecord record = paymentRecordMapper.selectOne(
                new LambdaQueryWrapper<PaymentRecord>()
                        .eq(PaymentRecord::getPaymentNo, paymentNo));
        if (record == null) {
            throw new BusinessException(ErrorCode.PAYMENT_NOT_FOUND);
        }

        // 2. 如果本地已是成功状态，直接返回（幂等性保护）
        if ("SUCCESS".equals(record.getStatus())) {
            log.info("Payment already SUCCESS locally, skip query: paymentNo={}", paymentNo);
            return Result.ok(toVO(record));
        }

        // 3. 向支付宝查询真实支付状态
        Map<String, String> result;
        try {
            result = alipayService.queryPayStatus(paymentNo);
        } catch (Exception e) {
            log.error("Failed to query Alipay for paymentNo={}: {}", paymentNo, e.getMessage());
            return Result.ok(toVO(record)); // 查询失败不阻塞，返回当前状态
        }

        String tradeStatus = result.get("trade_status");
        log.info("Alipay query response: paymentNo={}, tradeStatus={}", paymentNo, tradeStatus);

        // 4. 支付宝返回 TRADE_SUCCESS 且本地仍为 PENDING → 同步更新
        if ("TRADE_SUCCESS".equals(tradeStatus) && "PENDING".equals(record.getStatus())) {
            record.setStatus("SUCCESS");
            record.setPayTime(LocalDateTime.now());
            record.setCallbackData(JSON.toJSONString(result));
            record.setUpdateTime(LocalDateTime.now());
            paymentRecordMapper.updateById(record);

            // 通知订单服务更新状态
            Map<String, Object> statusBody = Map.of("status", "PENDING_SHIP");
            Result<Void> updateResult = orderFeignClient.updateStatus(record.getOrderId(), statusBody);
            if (updateResult.getCode() != 200) {
                log.warn("Failed to update order status via queryAlipayAndSync: paymentNo={}, orderId={}",
                        paymentNo, record.getOrderId());
            }

            log.info("Payment synced from Alipay: paymentNo={}, orderId={}", paymentNo, record.getOrderId());
        }

        return Result.ok(toVO(record));
    }

    private String generatePaymentNo() {
        long timestamp = System.currentTimeMillis();
        int random = (int) (Math.random() * 900000) + 100000;
        return "PAY" + timestamp + random;
    }

    private PaymentVO toVO(PaymentRecord record) {
        PaymentVO vo = new PaymentVO();
        vo.setId(record.getId());
        vo.setPaymentNo(record.getPaymentNo());
        vo.setOrderId(record.getOrderId());
        vo.setOrderNo(record.getOrderNo());
        vo.setUserId(record.getUserId());
        vo.setAmount(record.getAmount());
        vo.setPaymentMethod(record.getPaymentMethod());
        vo.setStatus(record.getStatus());
        vo.setCallbackData(record.getCallbackData());
        vo.setPayTime(record.getPayTime());
        vo.setCreateTime(record.getCreateTime());
        vo.setUpdateTime(record.getUpdateTime());
        return vo;
    }
}
