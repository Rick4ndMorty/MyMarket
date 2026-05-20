package com.tradestation.shop.controller;

import com.tradestation.common.result.Result;
import com.tradestation.shop.entity.CustomerMessage;
import com.tradestation.shop.service.CustomerMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/internal/shop/customer")
@RequiredArgsConstructor
public class AiMessageInternalController {

    private final CustomerMessageService customerMessageService;

    @PostMapping("/message/ai")
    public Result<CustomerMessage> sendAiMessage(@RequestBody Map<String, Object> body) {
        Long shopId = Long.valueOf(body.get("shopId").toString());
        Long userId = Long.valueOf(body.get("userId").toString());
        String content = (String) body.get("content");

        log.info("Internal AI message: shopId={}, userId={}", shopId, userId);
        return customerMessageService.sendAiMessage(shopId, userId, content);
    }
}
