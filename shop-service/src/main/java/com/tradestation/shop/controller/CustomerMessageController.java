package com.tradestation.shop.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tradestation.common.result.Result;
import com.tradestation.shop.entity.CustomerMessage;
import com.tradestation.shop.service.CustomerMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/shop/customer")
@RequiredArgsConstructor
public class CustomerMessageController {

    private final CustomerMessageService customerMessageService;

    @PostMapping("/message")
    public Result<CustomerMessage> send(@RequestHeader("X-User-Id") Long userId,
                                        @RequestBody Map<String, Object> body) {
        Long shopId = Long.valueOf(body.get("shopId").toString());
        String content = (String) body.get("content");
        String senderType = body.containsKey("senderType") ? (String) body.get("senderType") : "BUYER";
        Long targetUserId = body.containsKey("userId") ? Long.valueOf(body.get("userId").toString()) : userId;
        return customerMessageService.send(shopId, targetUserId, content, senderType);
    }

    @GetMapping("/message/list")
    public Result<Page<Map<String, Object>>> getMessages(@RequestParam("shopId") Long shopId,
                                                          @RequestParam(value = "userId", required = false) Long userId,
                                                          @RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = "20") int pageSize) {
        return customerMessageService.getMessages(shopId, userId, page, pageSize);
    }

    @PutMapping("/message/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        return customerMessageService.markRead(id);
    }

    @PutMapping("/message/read/{shopId}/{userId}")
    public Result<Void> markAllRead(@PathVariable Long shopId, @PathVariable Long userId) {
        return customerMessageService.markAllRead(shopId, userId);
    }

    @GetMapping("/conversations")
    public Result<List<Map<String, Object>>> getConversations(@RequestHeader("X-User-Id") Long userId) {
        return customerMessageService.getConversations(userId);
    }
}
