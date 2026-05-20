package com.tradestation.order.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tradestation.common.result.Result;
import com.tradestation.order.dto.CreateOrderReq;
import com.tradestation.order.dto.OrderDetailVO;
import com.tradestation.order.dto.OrderVO;

public interface OrderService {

    Result<OrderVO> createOrder(Long userId, CreateOrderReq req);

    Result<OrderDetailVO> getDetail(Long orderId, Long userId);

    Result<Page<OrderVO>> listBuyer(Long userId, String status, int page, int size);

    Result<Page<OrderVO>> listShop(Long shopId, String status, int page, int size);

    Result<Void> cancel(Long orderId, Long userId, String reason);

    Result<Void> ship(Long orderId, Long shopId);

    Result<Void> processRefund(Long orderId, Long shopId, boolean approve);

    Result<Void> confirmReceipt(Long orderId, Long userId);

    Result<OrderVO> getById(Long orderId);
}
