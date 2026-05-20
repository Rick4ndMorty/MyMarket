package com.tradestation.order.config;

import com.tradestation.order.dto.OrderEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Spring Cloud Stream 消息驱动配置
 * - 生产者：通过 StreamBridge 动态发送（在 OrderServiceImpl 中调用）
 * - 消费者：定义 Consumer bean 监听 order-events 队列
 */
@Configuration
public class OrderEventStreamConfig {

    private static final Logger log = LoggerFactory.getLogger(OrderEventStreamConfig.class);

    /**
     * 订单事件消息消费者
     * 监听 order-events 队列，收到消息后打印日志
     * 绑定名称：orderEvent-in-0 → 目标：order-events
     */
    @Bean
    public Consumer<Message<OrderEvent>> orderEvent() {
        return message -> {
            OrderEvent event = message.getPayload();
            log.info("📬 [Stream 消费者] 收到订单事件: {}", event);
            // 实际业务场景：更新店铺销量、发送通知、写入审计日志等
        };
    }
}