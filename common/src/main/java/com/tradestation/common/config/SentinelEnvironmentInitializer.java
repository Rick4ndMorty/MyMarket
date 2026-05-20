package com.tradestation.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * 在 Spring 环境就绪时提前设置 Sentinel 系统属性，
 * 确保 @PostConstruct init() 调用 InitExecutor.doInit() 时
 * csp.sentinel.app.name 和 csp.sentinel.dashboard.server 已可用。
 */
public class SentinelEnvironmentInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final Logger log = LoggerFactory.getLogger(SentinelEnvironmentInitializer.class);

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ConfigurableEnvironment env = applicationContext.getEnvironment();

        setPropertyIfMissing("csp.sentinel.app.name",
                env.getProperty("spring.application.name"));

        setPropertyIfMissing("csp.sentinel.dashboard.server",
                env.getProperty("spring.cloud.sentinel.transport.dashboard"));
    }

    private static void setPropertyIfMissing(String key, String value) {
        if (System.getProperty(key) == null && value != null && !value.isBlank()) {
            System.setProperty(key, value);
            log.info("Sentinel system property set: {}={}", key, value);
        }
    }
}
