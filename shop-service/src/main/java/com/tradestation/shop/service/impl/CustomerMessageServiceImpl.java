package com.tradestation.shop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tradestation.common.enums.ErrorCode;
import com.tradestation.common.exception.BusinessException;
import com.tradestation.common.result.Result;
import com.tradestation.shop.entity.CustomerMessage;
import com.tradestation.shop.entity.Shop;
import com.tradestation.shop.feign.ShopUserFeignClient;
import com.tradestation.shop.mapper.CustomerMessageMapper;
import com.tradestation.shop.mapper.ShopMapper;
import com.tradestation.shop.service.CustomerMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerMessageServiceImpl implements CustomerMessageService {

    private final CustomerMessageMapper customerMessageMapper;
    private final ShopMapper shopMapper;
    private final ShopUserFeignClient userFeignClient;

    @Override
    public Result<CustomerMessage> send(Long shopId, Long userId, String content, String senderType) {
        CustomerMessage message = new CustomerMessage();
        message.setShopId(shopId);
        message.setUserId(userId);
        message.setContent(content);
        message.setSenderType(senderType);
        message.setMessageType("CUSTOMER");
        message.setIsRead(0);
        message.setCreateTime(LocalDateTime.now());
        message.setUpdateTime(LocalDateTime.now());

        customerMessageMapper.insert(message);

        log.info("Customer message sent: shopId={}, userId={}, senderType={}", shopId, userId, senderType);
        return Result.ok(message);
    }

    @Override
    public Result<CustomerMessage> sendAiMessage(Long shopId, Long userId, String content) {
        CustomerMessage message = new CustomerMessage();
        message.setShopId(shopId);
        message.setUserId(userId);
        message.setContent(content);
        message.setSenderType("AI");
        message.setMessageType("AI");
        message.setIsRead(0);
        message.setCreateTime(LocalDateTime.now());
        message.setUpdateTime(LocalDateTime.now());

        customerMessageMapper.insert(message);

        log.info("AI message sent: shopId={}, userId={}", shopId, userId);
        return Result.ok(message);
    }

    @Override
    public Result<Page<Map<String, Object>>> getMessages(Long shopId, Long userId, int page, int size) {
        Page<CustomerMessage> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<CustomerMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerMessage::getShopId, shopId);
        if (userId != null) {
            wrapper.eq(CustomerMessage::getUserId, userId);
        }
        wrapper.orderByDesc(CustomerMessage::getCreateTime);

        Page<CustomerMessage> result = customerMessageMapper.selectPage(pageParam, wrapper);

        // 收集所有用户ID，批量查询用户信息
        Set<Long> userIds = result.getRecords().stream()
                .map(CustomerMessage::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, Map<String, Object>> userInfoMap = new HashMap<>();
        for (Long uid : userIds) {
            try {
                Result<Map<String, Object>> userResult = userFeignClient.getUserInfo(uid);
                if (userResult != null && userResult.getData() != null) {
                    userInfoMap.put(uid, userResult.getData());
                }
            } catch (Exception e) {
                log.warn("Failed to fetch user info for userId={}", uid, e);
            }
        }

        // 转换为 Map，携带 username 和 avatar
        List<Map<String, Object>> records = result.getRecords().stream().map(msg -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", msg.getId());
            map.put("shopId", msg.getShopId());
            map.put("userId", msg.getUserId());
            map.put("senderType", msg.getSenderType());
            map.put("messageType", msg.getMessageType());
            map.put("content", msg.getContent());
            map.put("isRead", msg.getIsRead());
            map.put("createTime", msg.getCreateTime());

            Map<String, Object> userInfo = userInfoMap.get(msg.getUserId());
            if (userInfo != null) {
                map.put("username", userInfo.get("username"));
                map.put("avatar", userInfo.get("avatar"));
            } else {
                map.put("username", null);
                map.put("avatar", null);
            }
            return map;
        }).collect(Collectors.toList());

        Page<Map<String, Object>> enriched = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        enriched.setRecords(records);
        return Result.ok(enriched);
    }

    @Override
    public Result<Void> markRead(Long messageId) {
        CustomerMessage message = customerMessageMapper.selectById(messageId);
        if (message == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        message.setIsRead(1);
        customerMessageMapper.updateById(message);

        return Result.ok();
    }

    @Override
    public Result<Void> markAllRead(Long shopId, Long userId) {
        LambdaQueryWrapper<CustomerMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerMessage::getShopId, shopId)
               .eq(CustomerMessage::getUserId, userId)
               .eq(CustomerMessage::getIsRead, 0);
        CustomerMessage update = new CustomerMessage();
        update.setIsRead(1);
        update.setUpdateTime(LocalDateTime.now());
        customerMessageMapper.update(update, wrapper);
        return Result.ok();
    }

    @Override
    public Result<List<Map<String, Object>>> getConversations(Long userId) {
        LambdaQueryWrapper<CustomerMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerMessage::getUserId, userId)
               .orderByDesc(CustomerMessage::getCreateTime);

        List<CustomerMessage> all = customerMessageMapper.selectList(wrapper);

        // 按 shopId 分组
        Map<Long, List<CustomerMessage>> grouped = all.stream()
                .collect(Collectors.groupingBy(CustomerMessage::getShopId, LinkedHashMap::new, Collectors.toList()));

        List<Map<String, Object>> conversations = new ArrayList<>();
        for (Map.Entry<Long, List<CustomerMessage>> entry : grouped.entrySet()) {
            Long shopId = entry.getKey();
            List<CustomerMessage> msgs = entry.getValue();
            CustomerMessage latest = msgs.get(0); // 最新消息（已按 createTime DESC 排序）

            long unread = msgs.stream()
                    .filter(m -> "SELLER".equals(m.getSenderType()) && m.getIsRead() != null && m.getIsRead() == 0)
                    .count();

            Map<String, Object> conv = new LinkedHashMap<>();
            conv.put("shopId", shopId);
            conv.put("lastMessage", latest.getContent());
            conv.put("lastMessageTime", latest.getCreateTime() != null ? latest.getCreateTime().toString() : null);
            conv.put("unreadCount", unread);

            try {
                Shop shop = shopMapper.selectById(shopId);
                if (shop != null) {
                    conv.put("shopName", shop.getShopName());
                    conv.put("shopLogo", shop.getLogo());
                }
            } catch (Exception e) {
                log.warn("Failed to fetch shop info for shopId={}", shopId, e);
            }

            conversations.add(conv);
        }

        // 按 lastMessageTime 倒序
        conversations.sort((a, b) -> {
            String ta = (String) a.get("lastMessageTime");
            String tb = (String) b.get("lastMessageTime");
            if (ta == null && tb == null) return 0;
            if (ta == null) return 1;
            if (tb == null) return -1;
            return tb.compareTo(ta);
        });

        return Result.ok(conversations);
    }
}
