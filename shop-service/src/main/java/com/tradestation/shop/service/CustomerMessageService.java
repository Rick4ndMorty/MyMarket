package com.tradestation.shop.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tradestation.common.result.Result;
import com.tradestation.shop.entity.CustomerMessage;

import java.util.List;
import java.util.Map;

public interface CustomerMessageService {

    Result<CustomerMessage> send(Long shopId, Long userId, String content, String senderType);

    Result<CustomerMessage> sendAiMessage(Long shopId, Long userId, String content);

    Result<Page<Map<String, Object>>> getMessages(Long shopId, Long userId, int page, int size);

    Result<Void> markRead(Long messageId);

    Result<Void> markAllRead(Long shopId, Long userId);

    Result<List<Map<String, Object>>> getConversations(Long userId);
}
