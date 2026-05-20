package com.tradestation.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tradestation.common.result.Result;
import com.tradestation.order.dto.CreateOrderReq;
import com.tradestation.order.dto.OrderDetailVO;
import com.tradestation.order.dto.OrderVO;
import com.tradestation.order.service.OrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/order")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public Result<OrderVO> create(@RequestHeader("X-User-Id") Long userId,
                                  @RequestBody CreateOrderReq req) {
        return orderService.createOrder(userId, req);
    }

    @GetMapping("/my")
    public Result<Page<OrderVO>> listBuyer(@RequestHeader("X-User-Id") Long userId,
                                           @RequestParam(required = false) String status,
                                           @RequestParam(defaultValue = "1") int page,
                                           @RequestParam(value = "page_size", defaultValue = "10") int size) {
        return orderService.listBuyer(userId, status, page, size);
    }

    @GetMapping("/shop")
    public Result<Page<OrderVO>> listShop(@RequestParam("shop_id") Long shopId,
                                          @RequestParam(required = false) String status,
                                          @RequestParam(defaultValue = "1") int page,
                                          @RequestParam(value = "page_size", defaultValue = "10") int size) {
        return orderService.listShop(shopId, status, page, size);
    }

    @GetMapping("/{id}")
    public Result<OrderDetailVO> getDetail(@PathVariable Long id,
                                           @RequestHeader("X-User-Id") Long userId) {
        return orderService.getDetail(id, userId);
    }

    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id,
                               @RequestHeader("X-User-Id") Long userId,
                               @RequestBody Map<String, String> body) {
        String reason = body.get("reason");
        return orderService.cancel(id, userId, reason);
    }

    @PutMapping("/{id}/ship")
    public Result<Void> ship(@PathVariable Long id,
                             @RequestParam Long shopId) {
        return orderService.ship(id, shopId);
    }

    @PutMapping("/{id}/refund")
    public Result<Void> processRefund(@PathVariable Long id,
                                      @RequestParam Long shopId,
                                      @RequestParam boolean approve) {
        return orderService.processRefund(id, shopId, approve);
    }

    @PutMapping("/{id}/complete")
    public Result<Void> confirmReceipt(@PathVariable Long id,
                                       @RequestHeader("X-User-Id") Long userId) {
        return orderService.confirmReceipt(id, userId);
    }
}
