package com.tradestation.order.service.impl;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradestation.common.enums.ErrorCode;
import com.tradestation.common.exception.BusinessException;
import com.tradestation.common.result.Result;
import com.tradestation.order.dto.CreateOrderReq;
import com.tradestation.order.dto.OrderDetailVO;
import com.tradestation.order.dto.OrderEvent;
import com.tradestation.order.dto.OrderVO;
import com.tradestation.order.entity.Order;
import com.tradestation.order.entity.OrderItem;
import com.tradestation.order.entity.OrderLog;
import com.tradestation.order.feign.ProductFeignClient;
import com.tradestation.order.feign.UserFeignClient;
import com.tradestation.order.mapper.OrderItemMapper;
import com.tradestation.order.mapper.OrderLogMapper;
import com.tradestation.order.mapper.OrderMapper;
import com.tradestation.order.service.OrderService;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    private static final String STATUS_PENDING_PAYMENT = "PENDING_PAYMENT";
    private static final String STATUS_PENDING_SHIP = "PENDING_SHIP";
    private static final String STATUS_SHIPPED = "SHIPPED";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String STATUS_REFUNDING = "REFUNDING";
    private static final String ORD_PREFIX = "ORD";

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderLogMapper orderLogMapper;
    private final UserFeignClient userFeignClient;
    private final ProductFeignClient productFeignClient;
    private final ObjectMapper objectMapper;
    private final StreamBridge streamBridge;

    public OrderServiceImpl(OrderMapper orderMapper,
                            OrderItemMapper orderItemMapper,
                            OrderLogMapper orderLogMapper,
                            UserFeignClient userFeignClient,
                            ProductFeignClient productFeignClient,
                            ObjectMapper objectMapper,
                            StreamBridge streamBridge) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.orderLogMapper = orderLogMapper;
        this.userFeignClient = userFeignClient;
        this.productFeignClient = productFeignClient;
        this.objectMapper = objectMapper;
        this.streamBridge = streamBridge;
    }

    // ---------------- createOrder ----------------

    @Override
    @GlobalTransactional(name = "createOrder", rollbackFor = Exception.class)
    @SentinelResource(value = "createOrder", blockHandler = "createOrderBlockHandler", fallback = "createOrderFallback")
    @Transactional(rollbackFor = Exception.class)
    public Result<OrderVO> createOrder(Long userId, CreateOrderReq req) {
        if (userId == null) {
            return Result.fail(ErrorCode.PARAM_ERROR, "userId is required");
        }
        if (req == null || CollectionUtils.isEmpty(req.getItems())) {
            return Result.fail(ErrorCode.PARAM_ERROR, "order items must not be empty");
        }
        if (req.getAddressId() == null) {
            return Result.fail(ErrorCode.PARAM_ERROR, "addressId is required");
        }

        // a) 获取地址快照
        Result<Map<String, Object>> addressResult = userFeignClient.getAddress(req.getAddressId());
        if (addressResult.getCode() != 200 || addressResult.getData() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "address not found");
        }
        Map<String, Object> addressData = addressResult.getData();

        // b) 收集所有 skuId，批量查询 SKU 信息
        List<Long> skuIds = req.getItems().stream()
                .map(CreateOrderReq.OrderItemReq::getSkuId)
                .distinct()
                .collect(Collectors.toList());
        String skuIdsStr = skuIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        Result<List<Map<String, Object>>> skuResult = productFeignClient.getSkuBatch(skuIdsStr);
        if (skuResult.getCode() != 200 || CollectionUtils.isEmpty(skuResult.getData())) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        List<Map<String, Object>> skuList = skuResult.getData();

        // 构建 skuId → SKU info 映射
        Map<Long, Map<String, Object>> skuMap = new HashMap<>();
        for (Map<String, Object> sku : skuList) {
            Object idObj = sku.get("id");
            if (idObj != null) {
                skuMap.put(((Number) idObj).longValue(), sku);
            }
        }

        // c) 校验所有 SKU 存在，且属于同一店铺
        Long shopId = null;
        for (Long skuId : skuIds) {
            Map<String, Object> sku = skuMap.get(skuId);
            if (sku == null) {
                throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "sku not found: " + skuId);
            }
            Object shopIdObj = sku.get("shopId");
            if (shopIdObj == null) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "sku has no shopId: " + skuId);
            }
            long currentShopId = ((Number) shopIdObj).longValue();
            if (shopId == null) {
                shopId = currentShopId;
            } else if (!shopId.equals(currentShopId)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "all items must belong to the same shop");
            }
        }

        // d) 计算总金额
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CreateOrderReq.OrderItemReq itemReq : req.getItems()) {
            Map<String, Object> sku = skuMap.get(itemReq.getSkuId());
            Object priceObj = sku.get("price");
            if (priceObj == null) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "sku has no price: " + itemReq.getSkuId());
            }
            BigDecimal price = new BigDecimal(priceObj.toString());
            totalAmount = totalAmount.add(price.multiply(BigDecimal.valueOf(itemReq.getQuantity())));
        }

        // e) 生成订单号
        String orderNo = generateOrderNo();

        // f) 保存订单
        LocalDateTime now = LocalDateTime.now();
        String addressSnapshotJson;
        try {
            addressSnapshotJson = objectMapper.writeValueAsString(addressData);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "failed to serialize address");
        }

        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setShopId(shopId);
        order.setTotalAmount(totalAmount);
        order.setPayAmount(totalAmount);
        order.setAddressSnapshot(addressSnapshotJson);
        order.setStatus(STATUS_PENDING_PAYMENT);
        order.setCreateTime(now);
        order.setUpdateTime(now);
        orderMapper.insert(order);

        // g) 保存订单项
        for (CreateOrderReq.OrderItemReq itemReq : req.getItems()) {
            Map<String, Object> sku = skuMap.get(itemReq.getSkuId());
            Object priceObj = sku.get("price");
            BigDecimal unitPrice = new BigDecimal(priceObj.toString());
            String skuSnapshotJson;
            try {
                skuSnapshotJson = objectMapper.writeValueAsString(sku);
            } catch (JsonProcessingException e) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "failed to serialize sku snapshot");
            }

            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setOrderNo(orderNo);
            item.setSkuId(itemReq.getSkuId());
            item.setSkuSnapshot(skuSnapshotJson);
            item.setQuantity(itemReq.getQuantity());
            item.setUnitPrice(unitPrice);
            item.setCreateTime(now);
            item.setUpdateTime(now);
            orderItemMapper.insert(item);
        }

        // h) 保存操作日志
        saveOrderLog(order.getId(), orderNo, null, STATUS_PENDING_PAYMENT, userId.toString(), req.getRemark());

        // i) 扣减库存
        List<Map<String, Object>> deductItems = new ArrayList<>();
        for (CreateOrderReq.OrderItemReq itemReq : req.getItems()) {
            Map<String, Object> sku = skuMap.get(itemReq.getSkuId());
            Map<String, Object> item = new HashMap<>();
            item.put("skuId", itemReq.getSkuId());
            item.put("quantity", itemReq.getQuantity());
            item.put("version", sku.get("version"));
            deductItems.add(item);
        }
        Result<Void> deductResult = productFeignClient.deductInventory(deductItems);
        if (deductResult.getCode() != 200) {
            throw new BusinessException(ErrorCode.STOCK_INSUFFICIENT, deductResult.getMessage());
        }

        // j) 发送订单创建事件到 RabbitMQ (Spring Cloud Stream)
        OrderEvent event = new OrderEvent(order.getId(), orderNo, userId, shopId, totalAmount, "CREATED");
        boolean sent = streamBridge.send("orderEvent-out-0", event);
        log.info("📤 [Stream 生产者] 订单创建事件发送{}: {}", sent ? "成功" : "失败", event);

        // k) 返回 OrderVO
        return Result.ok(toOrderVO(order));
    }

    /**
     * Sentinel block handler for createOrder
     */
    public Result<OrderVO> createOrderBlockHandler(Long userId, CreateOrderReq req, BlockException e) {
        log.error("(触发限流)createOrder blocked by Sentinel: userId={}", userId, e);
        return Result.fail(ErrorCode.RATE_LIMITED);
    }

    /**
     * Sentinel fallback for createOrder - 被调用服务异常或熔断时的降级兜底
     */
    public Result<OrderVO> createOrderFallback(Long userId, CreateOrderReq req, Throwable e) {
        log.error("(熔断)createOrder fallback: userId={}, error={}", userId, e.getMessage());
        return Result.fail(ErrorCode.CIRCUIT_BREAKER);
    }

    // ---------------- getDetail ----------------

    @Override
    public Result<OrderDetailVO> getDetail(Long orderId, Long userId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            return Result.fail(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!order.getUserId().equals(userId)) {
            return Result.fail(ErrorCode.FORBIDDEN, "not your order");
        }

        OrderDetailVO detail = new OrderDetailVO();
        copyOrderFields(order, detail);

        // 解析地址快照
        if (order.getAddressSnapshot() != null) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> addressMap = objectMapper.readValue(order.getAddressSnapshot(), Map.class);
                detail.setAddressSnapshot(addressMap);
            } catch (JsonProcessingException e) {
                log.error("failed to parse address snapshot for order {}", order.getId(), e);
                detail.setAddressSnapshot(new HashMap<>());
            }
        }

        // 查询订单项
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
        List<OrderDetailVO.OrderItemVO> itemVOList = new ArrayList<>();
        for (OrderItem item : items) {
            OrderDetailVO.OrderItemVO itemVO = new OrderDetailVO.OrderItemVO();
            itemVO.setId(item.getId());
            itemVO.setOrderId(item.getOrderId());
            itemVO.setSkuId(item.getSkuId());
            itemVO.setQuantity(item.getQuantity());
            itemVO.setUnitPrice(item.getUnitPrice());
            if (item.getSkuSnapshot() != null) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> skuMap = objectMapper.readValue(item.getSkuSnapshot(), Map.class);
                    itemVO.setSkuSnapshot(skuMap);
                } catch (JsonProcessingException e) {
                    log.error("failed to parse sku snapshot for order item {}", item.getId(), e);
                    itemVO.setSkuSnapshot(new HashMap<>());
                }
            }
            itemVOList.add(itemVO);
        }
        detail.setItems(itemVOList);

        return Result.ok(detail);
    }

    // ---------------- listBuyer ----------------

    @Override
    public Result<Page<OrderVO>> listBuyer(Long userId, String status, int page, int size) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Order::getStatus, status);
        }
        wrapper.orderByDesc(Order::getCreateTime);

        Page<Order> pageParam = new Page<>(page, size);
        Page<Order> orderPage = orderMapper.selectPage(pageParam, wrapper);

        Page<OrderVO> voPage = new Page<>(page, size, orderPage.getTotal());
        voPage.setRecords(orderPage.getRecords().stream()
                .map(this::toOrderVO)
                .collect(Collectors.toList()));

        return Result.ok(voPage);
    }

    // ---------------- listShop ----------------

    @Override
    public Result<Page<OrderVO>> listShop(Long shopId, String status, int page, int size) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getShopId, shopId);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Order::getStatus, status);
        }
        wrapper.orderByDesc(Order::getCreateTime);

        Page<Order> pageParam = new Page<>(page, size);
        Page<Order> orderPage = orderMapper.selectPage(pageParam, wrapper);

        Page<OrderVO> voPage = new Page<>(page, size, orderPage.getTotal());
        voPage.setRecords(orderPage.getRecords().stream()
                .map(this::toOrderVO)
                .collect(Collectors.toList()));

        return Result.ok(voPage);
    }

    // ---------------- cancel ----------------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> cancel(Long orderId, Long userId, String reason) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            return Result.fail(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!order.getUserId().equals(userId)) {
            return Result.fail(ErrorCode.FORBIDDEN, "not your order");
        }
        String fromStatus = order.getStatus();

        if (STATUS_PENDING_PAYMENT.equals(fromStatus)) {
            // 未支付直接取消，恢复库存
            order.setStatus(STATUS_CANCELLED);
            order.setCancelReason(reason);
            order.setUpdateTime(LocalDateTime.now());
            orderMapper.updateById(order);

            List<OrderItem> items = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
            if (!CollectionUtils.isEmpty(items)) {
                List<Map<String, Object>> restoreItems = new ArrayList<>();
                for (OrderItem item : items) {
                    Map<String, Object> ri = new HashMap<>();
                    ri.put("skuId", item.getSkuId());
                    ri.put("quantity", item.getQuantity());
                    restoreItems.add(ri);
                }
                Result<Void> restoreResult = productFeignClient.restoreInventory(restoreItems);
                if (restoreResult.getCode() == ErrorCode.SERVICE_DEGRADED.getCode()) {
                    log.error("(触发降级)restoreInventory degraded for order {}", orderId);
                    return Result.fail(ErrorCode.SERVICE_DEGRADED);
                }
                if (restoreResult.getCode() != 200) {
                    log.error("restoreInventory failed for order {}: {}", orderId, restoreResult.getMessage());
                    throw new BusinessException(ErrorCode.INTERNAL_ERROR, "failed to restore inventory");
                }
            }

            saveOrderLog(order.getId(), order.getOrderNo(), fromStatus, STATUS_CANCELLED, userId.toString(), reason);
        } else if (STATUS_PENDING_SHIP.equals(fromStatus)) {
            // 已支付 → 发起退款申请
            order.setStatus(STATUS_REFUNDING);
            order.setCancelReason(reason);
            order.setUpdateTime(LocalDateTime.now());
            orderMapper.updateById(order);

            saveOrderLog(order.getId(), order.getOrderNo(), fromStatus, STATUS_REFUNDING, userId.toString(), reason);
        } else {
            return Result.fail(ErrorCode.ORDER_STATUS_INVALID, "cannot cancel order in " + fromStatus + " status");
        }

        return Result.ok();
    }

    // ---------------- ship ----------------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> ship(Long orderId, Long shopId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            return Result.fail(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!order.getShopId().equals(shopId)) {
            return Result.fail(ErrorCode.FORBIDDEN, "not your shop's order");
        }
        if (!STATUS_PENDING_SHIP.equals(order.getStatus())) {
            return Result.fail(ErrorCode.ORDER_STATUS_INVALID, "order is not pending shipment");
        }

        return updateStatus(orderId, STATUS_SHIPPED);
    }

    // ---------------- processRefund ----------------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> processRefund(Long orderId, Long shopId, boolean approve) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            return Result.fail(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!order.getShopId().equals(shopId)) {
            return Result.fail(ErrorCode.FORBIDDEN, "not your shop's order");
        }
        if (!STATUS_REFUNDING.equals(order.getStatus())) {
            return Result.fail(ErrorCode.ORDER_STATUS_INVALID, "order is not in refunding status");
        }

        String fromStatus = order.getStatus();
        if (approve) {
            // 同意退款 → 取消订单 + 恢复库存
            order.setStatus(STATUS_CANCELLED);
            order.setUpdateTime(LocalDateTime.now());
            orderMapper.updateById(order);

            List<OrderItem> items = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
            if (!CollectionUtils.isEmpty(items)) {
                List<Map<String, Object>> restoreItems = new ArrayList<>();
                for (OrderItem item : items) {
                    Map<String, Object> ri = new HashMap<>();
                    ri.put("skuId", item.getSkuId());
                    ri.put("quantity", item.getQuantity());
                    restoreItems.add(ri);
                }
                Result<Void> restoreResult = productFeignClient.restoreInventory(restoreItems);
                if (restoreResult.getCode() == ErrorCode.SERVICE_DEGRADED.getCode()) {
                    log.error("(触发降级)restoreInventory degraded for order {}", orderId);
                    return Result.fail(ErrorCode.SERVICE_DEGRADED);
                }
                if (restoreResult.getCode() != 200) {
                    log.error("restoreInventory failed for order {}: {}", orderId, restoreResult.getMessage());
                    throw new BusinessException(ErrorCode.INTERNAL_ERROR, "failed to restore inventory");
                }
            }

            saveOrderLog(order.getId(), order.getOrderNo(), fromStatus, STATUS_CANCELLED, "SELLER", "refund approved");
        } else {
            // 拒绝退款 → 回到待发货
            order.setStatus(STATUS_PENDING_SHIP);
            order.setCancelReason(null);
            order.setUpdateTime(LocalDateTime.now());
            orderMapper.updateById(order);

            saveOrderLog(order.getId(), order.getOrderNo(), fromStatus, STATUS_PENDING_SHIP, "SELLER", "refund rejected");
        }

        return Result.ok();
    }

    // ---------------- confirmReceipt ----------------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> confirmReceipt(Long orderId, Long userId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            return Result.fail(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!order.getUserId().equals(userId)) {
            return Result.fail(ErrorCode.FORBIDDEN, "not your order");
        }
        if (!STATUS_SHIPPED.equals(order.getStatus())) {
            return Result.fail(ErrorCode.ORDER_STATUS_INVALID, "order is not shipped yet");
        }

        String fromStatus = order.getStatus();
        order.setStatus(STATUS_COMPLETED);
        order.setCompleteTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);

        saveOrderLog(order.getId(), order.getOrderNo(), fromStatus, STATUS_COMPLETED, userId.toString(), "buyer confirmed receipt");

        return Result.ok();
    }

    // ---------------- getById (内部 Feign 使用) ----------------

    @Override
    public Result<OrderVO> getById(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            return Result.fail(ErrorCode.ORDER_NOT_FOUND);
        }
        return Result.ok(toOrderVO(order));
    }

    // ---------------- 内部方法：更新订单状态（支付回调用） ----------------

    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateStatus(Long orderId, String status) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            return Result.fail(ErrorCode.ORDER_NOT_FOUND);
        }

        String fromStatus = order.getStatus();

        // 状态流转校验
        if (STATUS_PENDING_PAYMENT.equals(fromStatus) && STATUS_PENDING_SHIP.equals(status)) {
            order.setPayTime(LocalDateTime.now());
        } else if (STATUS_PENDING_SHIP.equals(fromStatus) && STATUS_SHIPPED.equals(status)) {
            order.setShipTime(LocalDateTime.now());
        } else if (STATUS_SHIPPED.equals(fromStatus) && STATUS_COMPLETED.equals(status)) {
            order.setCompleteTime(LocalDateTime.now());
        } else {
            return Result.fail(ErrorCode.ORDER_STATUS_INVALID,
                    "cannot transition from " + fromStatus + " to " + status);
        }

        order.setStatus(status);
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);

        saveOrderLog(order.getId(), order.getOrderNo(), fromStatus, status, "SYSTEM", null);

        return Result.ok();
    }

    // ---------------- 私有辅助方法 ----------------

    private String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomDigits = String.format("%04d", new Random().nextInt(10000));
        return ORD_PREFIX + timestamp + randomDigits;
    }

    private void saveOrderLog(Long orderId, String orderNo, String fromStatus, String toStatus,
                              String operator, String remark) {
        OrderLog logEntity = new OrderLog();
        logEntity.setOrderId(orderId);
        logEntity.setOrderNo(orderNo);
        logEntity.setFromStatus(fromStatus);
        logEntity.setToStatus(toStatus);
        logEntity.setOperator(operator);
        logEntity.setRemark(remark);
        logEntity.setCreateTime(LocalDateTime.now());
        logEntity.setUpdateTime(LocalDateTime.now());
        orderLogMapper.insert(logEntity);
    }

    private OrderVO toOrderVO(Order order) {
        OrderVO vo = new OrderVO();
        copyOrderFields(order, vo);
        return vo;
    }

    private void copyOrderFields(Order order, OrderVO vo) {
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId());
        vo.setShopId(order.getShopId());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setPayAmount(order.getPayAmount());
        vo.setStatus(order.getStatus());
        vo.setCancelReason(order.getCancelReason());
        vo.setPayTime(order.getPayTime());
        vo.setShipTime(order.getShipTime());
        vo.setCompleteTime(order.getCompleteTime());
        vo.setCreateTime(order.getCreateTime());
    }
}
