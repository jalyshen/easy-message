package com.easy.messaging.autoconfig;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "easy-messaging")
public class EasyMessagingProperties {

    /**
     * default is turn on message function
     */
    private boolean enabled = true;

    /**
     * logical destination → maps to topic/hub/queue
     * on business code, using destination only
     */
    private Map<String, String> destinations = new HashMap<>();

    /**
     * the prefix for consumer id，
     * the default value when generates a subscriberId。
     */
    private String subscriberPrefix = "easy-consumer-";

    /**
     * 是否启用消息 trace。
     */
    private boolean tracingEnabled = true;

    /**
     * the thread numbers for consumers
     */
    private int consumerConcurrency = 1;

    /**
     * the interval time (millisecond) between 2 re-try
     */
    private long retryInterval = 3000;

    public boolean isEnabled() {
        return this.enabled;
    }

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
     * to resolve destination，
     * return the original value if the target value is not found
     */
    public String resolveDestination(String logical) {
        return destinations.getOrDefault(logical, logical);
    }
}
