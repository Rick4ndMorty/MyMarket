package com.tradestation.order.controller;

import com.tradestation.common.result.Result;
import com.tradestation.order.dto.OrderVO;
import com.tradestation.order.service.OrderService;
import com.tradestation.order.service.impl.OrderServiceImpl;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/order/internal")
public class OrderInternalController {

    private final OrderService orderService;
    private final OrderServiceImpl orderServiceImpl;

    public OrderInternalController(OrderService orderService, OrderServiceImpl orderServiceImpl) {
        this.orderService = orderService;
        this.orderServiceImpl = orderServiceImpl;
    }

    @GetMapping("/{id}")
    public Result<OrderVO> getById(@PathVariable Long id) {
        return orderService.getById(id);
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @RequestBody Map<String, Object> body) {
        Object statusObj = body.get("status");
        if (statusObj == null) {
            return Result.fail(422, "status is required");
        }
        return orderServiceImpl.updateStatus(id, statusObj.toString());
    }
}
