package com.tradestation.order.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tradestation.common.enums.ErrorCode;
import com.tradestation.common.exception.BusinessException;
import com.tradestation.common.result.Result;
import com.tradestation.order.entity.Order;
import com.tradestation.order.entity.OrderItem;
import com.tradestation.order.mapper.OrderMapper;
import com.tradestation.order.mapper.OrderItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/order/admin")
@RequiredArgsConstructor
public class OrderAdminController {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;

    private void checkAdmin(String role) {
        if (!"ADMIN".equals(role)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats(@RequestHeader("X-User-Role") String role) {
        checkAdmin(role);
        Map<String, Object> result = new HashMap<>();
        java.util.List<Order> all = orderMapper.selectList(null);
        result.put("totalOrders", all.size());
        Map<String, Long> byStatus = new HashMap<>();
        BigDecimal totalRevenue = BigDecimal.ZERO;
        for (Order o : all) {
            byStatus.merge(o.getStatus(), 1L, Long::sum);
            if (o.getPayAmount() != null) {
                totalRevenue = totalRevenue.add(o.getPayAmount());
            }
        }
        result.put("byStatus", byStatus);
        result.put("totalRevenue", totalRevenue.doubleValue());
        return Result.ok(result);
    }

    @GetMapping("/orders")
    public Result<Page<Order>> listOrders(
            @RequestHeader("X-User-Role") String role,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long shopId,
            @RequestParam(required = false) String status) {
        checkAdmin(role);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(orderNo)) {
            wrapper.like(Order::getOrderNo, orderNo);
        }
        if (userId != null) {
            wrapper.eq(Order::getUserId, userId);
        }
        if (shopId != null) {
            wrapper.eq(Order::getShopId, shopId);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(Order::getStatus, status);
        }
        wrapper.orderByDesc(Order::getCreateTime);
        return Result.ok(orderMapper.selectPage(new Page<>(page, size), wrapper));
    }

    @PutMapping("/orders/{id}/status")
    public Result<Void> updateStatus(
            @RequestHeader("X-User-Role") String role,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        checkAdmin(role);
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        String newStatus = body.get("status");
        order.setStatus(newStatus);
        if ("PENDING_SHIP".equals(newStatus)) {
            order.setPayTime(LocalDateTime.now());
        } else if ("SHIPPED".equals(newStatus)) {
            order.setShipTime(LocalDateTime.now());
        } else if ("COMPLETED".equals(newStatus)) {
            order.setCompleteTime(LocalDateTime.now());
        }
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        log.info("Admin updated order status: id={}, status={}", id, newStatus);
        return Result.ok();
    }

    // 获取订单详情（含订单商品）
    @GetMapping("/orders/{id}")
    public Result<Map<String, Object>> getOrderDetail(
            @RequestHeader("X-User-Role") String role,
            @PathVariable Long id) {
        checkAdmin(role);
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        // 查询订单商品
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OrderItem> itemWrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        itemWrapper.eq(OrderItem::getOrderId, id);
        java.util.List<OrderItem> items = orderItemMapper.selectList(itemWrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("order", order);
        result.put("items", items);
        return Result.ok(result);
    }

    // 订单退款
    @PutMapping("/orders/{id}/refund")
    public Result<Void> refundOrder(
            @RequestHeader("X-User-Role") String role,
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        checkAdmin(role);
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if ("REFUNDED".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "订单已退款");
        }
        String reason = body != null ? body.getOrDefault("reason", "管理员退款") : "管理员退款";
        order.setStatus("REFUNDED");
        order.setCancelReason(reason);
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        log.info("Admin refunded order: id={}, reason={}", id, reason);
        return Result.ok();
    }
}
