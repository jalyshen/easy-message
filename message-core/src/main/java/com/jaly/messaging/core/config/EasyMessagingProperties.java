package com.jaly.messaging.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "easy-messaging")
public class EasyMessagingProperties {

    /**
     * 逻辑 destination → MQ 实际 topic/hub/queue 映射。
     * 业务层只使用逻辑名。
     */
    private Map<String, String> destinations = new HashMap<>();

    /**
     * 消费者 ID 前缀，用于生成默认 subscriberId。
     */
    private String subscriberPrefix = "easy-consumer-";

    /**
     * 是否启用消息 trace。
     */
    private boolean tracingEnabled = true;

    /**
     * 消费线程数或并发度（由具体 MQ 实现决定如何使用）。
     */
    private int consumerConcurrency = 1;

    /**
     * 消费重试间隔（毫秒）。
     */
    private long retryInterval = 3000;

    // getters & setters
    public Map<String, String> getDestinations() {
        return destinations;
    }

    public void setDestinations(Map<String, String> destinations) {
        this.destinations = destinations;
    }

    public String getSubscriberPrefix() {
        return subscriberPrefix;
    }

    public void setSubscriberPrefix(String subscriberPrefix) {
        this.subscriberPrefix = subscriberPrefix;
    }

    public boolean isTracingEnabled() {
        return tracingEnabled;
    }

    public void setTracingEnabled(boolean tracingEnabled) {
        this.tracingEnabled = tracingEnabled;
    }

    public int getConsumerConcurrency() {
        return consumerConcurrency;
    }

    public void setConsumerConcurrency(int consumerConcurrency) {
        this.consumerConcurrency = consumerConcurrency;
    }

    public long getRetryInterval() {
        return retryInterval;
    }

    public void setRetryInterval(long retryInterval) {
        this.retryInterval = retryInterval;
    }

    /**
     * 解析逻辑 destination，找不到时返回原值。
     */
    public String resolveDestination(String logical) {
        return destinations.getOrDefault(logical, logical);
    }
}
