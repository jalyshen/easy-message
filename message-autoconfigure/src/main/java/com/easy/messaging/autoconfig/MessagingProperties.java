package com.easy.messaging.autoconfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * yaml file sample:
 * =====================================
 * message:
 *   defaults:
 *     producer:
 *       max-attempts: 3
 *       initial-interval-ms: 2000
 *       multiplier: 2.0
 *       max-interval-ms: 60000
 *     consumer:
 *       max-concurrent-calls: 20
 *       prefetch-count: 60
 *
 *   instances:
 *     orders:
 *       enabled: true
 *       event-hub-name: "orders"
 *
 *       producer-connector:
 *         enabled: true
 *         connection-string: "${EVENTHUB_ORDERS_SEND_CONNECTION_STRING}"
 *         retry:
 *           max-attempts: 5
 *           initial-interval-ms: 800
 *           multiplier: 2.0
 *           max-interval-ms: 45000
 *
 *       consumer-connector:
 *         enabled: true
 *         connection-string: "${EVENTHUB_ORDERS_LISTEN_CONNECTION_STRING}"
 *         consumer-group: "order-processing-service"
 *         concurrency:
 *           max-concurrent-calls: 32
 *           prefetch-count: 90
 *
 *     payments:
 *       enabled: true
 *       event-hub-name: "payments"
 *       producer-connector:
 *         enabled: true
 *         connection-string: "${EVENTHUB_PAYMENTS_SEND_CONNECTION_STRING}"
 *
 *     notifications:
 *       enabled: true
 *       event-hub-name: "notifications"
 *       consumer-connector:
 *         enabled: true
 *         connection-string: "${EVENTHUB_NOTIFICATIONS_LISTEN_CONNECTION_STRING}"
 *         consumer-group: "notification-delivery-service"
 *
 *     audit-logs:
 *       enabled: true
 *       producer-connector:
 *         enabled: true
 *         connection-string: "${EVENTHUB_AUDIT_LOGS_SEND_CONNECTION_STRING}"
 *         event-hub-name: "audit-logs-ingest"
 *       consumer-connector:
 *         enabled: true
 *         connection-string: "${EVENTHUB_AUDIT_LOGS_LISTEN_CONNECTION_STRING}"
 *         consumer-group: "audit-analytics-service"
 *         concurrency:
 *           max-concurrent-calls: 15
 *           prefetch-count: 40
 *
 *     telemetry:
 *       enabled: true
 *       event-hub-name: "device-telemetry"
 *       producer-connector:
 *         enabled: true
 *         connection-string: "${EVENTHUB_TELEMETRY_SEND_CONNECTION_STRING}"
 *       consumer-connector:
 *         enabled: true
 *         connection-string: "${EVENTHUB_TELEMETRY_LISTEN_CONNECTION_STRING}"
 *         consumer-group: "telemetry-processor"
 */

@Component
@ConfigurationProperties(prefix = "message")
public class MessagingProperties {

    private static final Logger logger = LoggerFactory.getLogger(MessagingProperties.class);

    private Defaults defaults = new Defaults();
    private Map<String, EventHubInstance> instances = new LinkedHashMap<>();

    // Azure Blob Storage for Checkpoint (Event Hub Consumer 必须)
    private String checkpointConnectionString;
    private String checkpointContainerName;

    // Getters Setters for checkpoint config
    public String getCheckpointConnectionString() { return checkpointConnectionString; }
    public void setCheckpointConnectionString(String checkpointConnectionString) { this.checkpointConnectionString = checkpointConnectionString; }
    public String getCheckpointContainerName() { return checkpointContainerName; }
    public void setCheckpointContainerName(String checkpointContainerName) { this.checkpointContainerName = checkpointContainerName; }

    // Getter and Setter
    public Defaults getDefaults() {
        return defaults;
    }

    public void setDefaults(Defaults defaults) {
        this.defaults = defaults;
    }

    public Map<String, EventHubInstance> getInstances() {
        return instances;
    }

    public void setInstances(Map<String, EventHubInstance> instances) {
        this.instances = instances;
    }

    /**
     * 将逻辑 Destination (如 "CreateOrder") 解析为物理 Topic 名 (如 "orders")。
     * 查找顺序:
     * 1. defaults.destinations map
     * 2. 如果没找到，返回原名
     */
    public String resolveDestination(String logicalName) {
        if (this.defaults != null && this.defaults.getDestinations() != null) {
            return this.defaults.getDestinations().getOrDefault(logicalName, logicalName);
        }
        return logicalName;
    }


    public static class Defaults {
        private ProducerDefaults producer = new ProducerDefaults();
        private ConsumerDefaults consumer = new ConsumerDefaults();

        private Map<String, String> destinations = new LinkedHashMap<>();

        public ProducerDefaults getProducer() {
            return producer;
        }

        public void setProducer(ProducerDefaults producer) {
            this.producer = producer;
        }

        public ConsumerDefaults getConsumer() {
            return consumer;
        }

        public void setConsumer(ConsumerDefaults consumer) {
            this.consumer = consumer;
        }

        public Map<String, String> getDestinations() {
            return destinations;
        }

        public void setDestinations(Map<String, String> destinations) {
            this.destinations = destinations;
        }
    }

    public static class ProducerDefaults {
        private Integer maxAttempts = 10;
        private Long initialIntervalMs = 1000L;
        private Double multiplier = 8.0;
        private Long maxIntervalMs = 300000L;

        public Integer getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(Integer maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public Long getInitialIntervalMs() {
            return initialIntervalMs;
        }

        public void setInitialIntervalMs(Long initialIntervalMs) {
            this.initialIntervalMs = initialIntervalMs;
        }

        public Double getMultiplier() {
            return multiplier;
        }

        public void setMultiplier(Double multiplier) {
            this.multiplier = multiplier;
        }

        public Long getMaxIntervalMs() {
            return maxIntervalMs;
        }

        public void setMaxIntervalMs(Long maxIntervalMs) {
            this.maxIntervalMs = maxIntervalMs;
        }
    }


    public static class ConsumerDefaults {
        private Integer maxConcurrentCalls = 16;
        private Integer prefetchCount = 50;

        public Integer getMaxConcurrentCalls() {
            return maxConcurrentCalls;
        }

        public void setMaxConcurrentCalls(Integer maxConcurrentCalls) {
            this.maxConcurrentCalls = maxConcurrentCalls;
        }

        public Integer getPrefetchCount() {
            return prefetchCount;
        }

        public void setPrefetchCount(Integer prefetchCount) {
            this.prefetchCount = prefetchCount;
        }
    }


    public static class EventHubInstance {
        private Boolean enabled = true;
        private String eventHubName;

        private ProducerConnector producerConnector;
        private ConsumerConnector consumerConnector;

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public String getEventHubName() {
            return eventHubName;
        }

        public void setEventHubName(String eventHubName) {
            this.eventHubName = eventHubName;
        }

        public ProducerConnector getProducerConnector() {
            return producerConnector;
        }

        public void setProducerConnector(ProducerConnector producerConnector) {
            this.producerConnector = producerConnector;
        }

        public ConsumerConnector getConsumerConnector() {
            return consumerConnector;
        }

        public void setConsumerConnector(ConsumerConnector consumerConnector) {
            this.consumerConnector = consumerConnector;
        }

        public String getProducerConnectionString() {
            if (producerConnector != null && producerConnector.getConnectionString() != null) {
                return producerConnector.getConnectionString();
            }
            return null;
        }

        public String getConsumerConnectionString() {
            if (consumerConnector != null && consumerConnector.getConnectionString() != null) {
                return consumerConnector.getConnectionString();
            }
            return null;
        }
    }

    public static class ProducerConnector {
        private Boolean enabled = true;
        private String connectionString;
        private RetryConfig retry;

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public String getConnectionString() {
            return connectionString;
        }

        public void setConnectionString(String connectionString) {
            this.connectionString = connectionString;
        }

        public RetryConfig getRetry() {
            return retry;
        }

        public void setRetry(RetryConfig retry) {
            this.retry = retry;
        }
    }


    public static class ConsumerConnector {
        private Boolean enabled = true;
        private String connectionString;
        private String consumerGroup = "$Default";
        private ConcurrencyConfig concurrency;

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public String getConnectionString() {
            return connectionString;
        }

        public void setConnectionString(String connectionString) {
            this.connectionString = connectionString;
        }

        public String getConsumerGroup() {
            return consumerGroup;
        }

        public void setConsumerGroup(String consumerGroup) {
            this.consumerGroup = consumerGroup;
        }

        public ConcurrencyConfig getConcurrency() {
            return concurrency;
        }

        public void setConcurrency(ConcurrencyConfig concurrency) {
            this.concurrency = concurrency;
        }
    }


    public static class RetryConfig {
        private Integer maxAttempts;
        private Long initialIntervalMs;
        private Double multiplier;
        private Long maxIntervalMs;

        public Integer getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(Integer maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public Long getInitialIntervalMs() {
            return initialIntervalMs;
        }

        public void setInitialIntervalMs(Long initialIntervalMs) {
            this.initialIntervalMs = initialIntervalMs;
        }

        public Double getMultiplier() {
            return multiplier;
        }

        public void setMultiplier(Double multiplier) {
            this.multiplier = multiplier;
        }

        public Long getMaxIntervalMs() {
            return maxIntervalMs;
        }

        public void setMaxIntervalMs(Long maxIntervalMs) {
            this.maxIntervalMs = maxIntervalMs;
        }
    }


    public static class ConcurrencyConfig {
        private Integer maxConcurrentCalls;
        private Integer prefetchCount;

        public Integer getMaxConcurrentCalls() {
            return maxConcurrentCalls;
        }

        public void setMaxConcurrentCalls(Integer maxConcurrentCalls) {
            this.maxConcurrentCalls = maxConcurrentCalls;
        }

        public Integer getPrefetchCount() {
            return prefetchCount;
        }

        public void setPrefetchCount(Integer prefetchCount) {
            this.prefetchCount = prefetchCount;
        }
    }


    public static class ProducerInfo {
        private String businessName;
        private String connectionString;
        private String eventHubName;
        private RetryConfig retryConfig;

        public String getBusinessName() {
            return businessName;
        }

        public void setBusinessName(String businessName) {
            this.businessName = businessName;
        }

        public String getConnectionString() {
            return connectionString;
        }

        public void setConnectionString(String connectionString) {
            this.connectionString = connectionString;
        }

        public String getEventHubName() {
            return eventHubName;
        }

        public void setEventHubName(String eventHubName) {
            this.eventHubName = eventHubName;
        }

        public RetryConfig getRetryConfig() {
            return retryConfig;
        }

        public void setRetryConfig(RetryConfig retryConfig) {
            this.retryConfig = retryConfig;
        }
    }


    public static class ConsumerInfo {
        private String businessName;
        private String connectionString;
        private String eventHubName;
        private String consumerGroup;
        private ConcurrencyConfig concurrencyConfig;

        public String getBusinessName() {
            return businessName;
        }

        public void setBusinessName(String businessName) {
            this.businessName = businessName;
        }

        public String getConnectionString() {
            return connectionString;
        }

        public void setConnectionString(String connectionString) {
            this.connectionString = connectionString;
        }

        public String getEventHubName() {
            return eventHubName;
        }

        public void setEventHubName(String eventHubName) {
            this.eventHubName = eventHubName;
        }

        public String getConsumerGroup() {
            return consumerGroup;
        }

        public void setConsumerGroup(String consumerGroup) {
            this.consumerGroup = consumerGroup;
        }

        public ConcurrencyConfig getConcurrencyConfig() {
            return concurrencyConfig;
        }

        public void setConcurrencyConfig(ConcurrencyConfig concurrencyConfig) {
            this.concurrencyConfig = concurrencyConfig;
        }
    }


    public Map<String, ProducerInfo> getEnabledProducers() {
        Map<String, ProducerInfo> producers = new LinkedHashMap<>();

        for (Map.Entry<String, EventHubInstance> entry : instances.entrySet()) {
            String businessName = entry.getKey();
            EventHubInstance instance = entry.getValue();

            boolean isEnableConnecting = Boolean.TRUE.equals(instance.getEnabled())
                    && instance.getProducerConnector() != null
                    && Boolean.TRUE.equals(instance.getProducerConnector().getEnabled());

            if (isEnableConnecting) {

                String connStr = instance.getProducerConnectionString();
                if (connStr == null || connStr.trim().isEmpty()) {
                    logger.warn("Producer enabled but no connection string configured for business: {}", businessName);
                    continue;
                }

                String eventHubName = instance.getEventHubName();
                if (eventHubName == null || eventHubName.trim().isEmpty()) {
                    logger.warn("Producer enabled but no event hub name configured for business: {}", businessName);
                    continue;
                }

                ProducerInfo info = new ProducerInfo();
                info.setBusinessName(businessName);
                info.setConnectionString(connStr);
                info.setEventHubName(eventHubName);
                info.setRetryConfig(resolveRetryConfig(instance));
                producers.put(businessName, info);
            }
        }

        logger.info("Found {} enabled producers: {}", producers.size(), producers.keySet());
        return producers;
    }

    public Map<String, ConsumerInfo> getEnabledConsumers() {
        Map<String, ConsumerInfo> consumers = new LinkedHashMap<>();

        for (Map.Entry<String, EventHubInstance> entry : instances.entrySet()) {
            String businessName = entry.getKey();
            EventHubInstance instance = entry.getValue();

            boolean isEnableConnecting = Boolean.TRUE.equals(instance.getEnabled())
                    && instance.getConsumerConnector() != null
                    && Boolean.TRUE.equals(instance.getConsumerConnector().getEnabled());

            if (isEnableConnecting) {

                String connStr = instance.getConsumerConnectionString();
                if (connStr == null || connStr.trim().isEmpty()) {
                    logger.warn("Consumer enabled but no connection string configured for business: {}", businessName);
                    continue;
                }

                String eventHubName = instance.getEventHubName();
                if (eventHubName == null || eventHubName.trim().isEmpty()) {
                    logger.warn("Consumer enabled but no event hub name configured for business: {}", businessName);
                    continue;
                }

                ConsumerInfo info = new ConsumerInfo();
                info.setBusinessName(businessName);
                info.setConnectionString(connStr);
                info.setEventHubName(eventHubName);
                info.setConsumerGroup(resolveConsumerGroup(instance));
                info.setConcurrencyConfig(resolveConcurrencyConfig(instance));
                consumers.put(businessName, info);
            }
        }

        logger.info("Found {} enabled consumers: {}", consumers.size(), consumers.keySet());
        return consumers;
    }


    private RetryConfig resolveRetryConfig(EventHubInstance instance) {
        RetryConfig instanceRetry = instance.getProducerConnector() != null ?
                instance.getProducerConnector().getRetry() : null;

        RetryConfig result = new RetryConfig();

        result.setMaxAttempts(getValueOrDefault(
                instanceRetry != null ? instanceRetry.getMaxAttempts() : null,
                defaults.getProducer().getMaxAttempts()));

        result.setInitialIntervalMs(getValueOrDefault(
                instanceRetry != null ? instanceRetry.getInitialIntervalMs() : null,
                defaults.getProducer().getInitialIntervalMs()));

        result.setMultiplier(getValueOrDefault(
                instanceRetry != null ? instanceRetry.getMultiplier() : null,
                defaults.getProducer().getMultiplier()));

        result.setMaxIntervalMs(getValueOrDefault(
                instanceRetry != null ? instanceRetry.getMaxIntervalMs() : null,
                defaults.getProducer().getMaxIntervalMs()));

        return result;
    }


    private String resolveConsumerGroup(EventHubInstance instance) {
        if (instance.getConsumerConnector() != null &&
                instance.getConsumerConnector().getConsumerGroup() != null) {
            return instance.getConsumerConnector().getConsumerGroup();
        }
        return "$Default";
    }


    private ConcurrencyConfig resolveConcurrencyConfig(EventHubInstance instance) {
        ConcurrencyConfig instanceConcurrency = instance.getConsumerConnector() != null
                ? instance.getConsumerConnector().getConcurrency()
                : null;

        ConcurrencyConfig result = new ConcurrencyConfig();

        result.setMaxConcurrentCalls(getValueOrDefault(
                instanceConcurrency != null
                        ? instanceConcurrency.getMaxConcurrentCalls()
                        : null, defaults.getConsumer().getMaxConcurrentCalls()));

        result.setPrefetchCount(getValueOrDefault(
                instanceConcurrency != null
                        ? instanceConcurrency.getPrefetchCount()
                        : null, defaults.getConsumer().getPrefetchCount()));

        return result;
    }

    @SuppressWarnings("unchecked")
    private <T> T getValueOrDefault(T instanceValue, T defaultValue) {
        return instanceValue != null ? instanceValue : defaultValue;
    }

    public boolean validate() {
        Map<String, ProducerInfo> producers = getEnabledProducers();
        Map<String, ConsumerInfo> consumers = getEnabledConsumers();

        if (producers.isEmpty() && consumers.isEmpty()) {
            logger.warn("No enabled producers or consumers found in configuration");
            return false;
        }

        if (!producers.isEmpty()) {
            logger.info("Validated {} producers", producers.size());
        }

        if (!consumers.isEmpty()) {
            logger.info("Validated {} consumers", consumers.size());
        }

        return true;
    }


    // 辅助方法：根据 EventHubName 查找配置
    // 这对于 Consumer 初始化很有用，因为 @MessageListener(destination="xxx") 需要反向查找是哪个 EventHubInstance
    public ConsumerInfo findConsumerInfoByTopic(String topic) {
        for (ConsumerInfo info : getEnabledConsumers().values()) {
            if (info.getEventHubName().equalsIgnoreCase(topic)) {
                return info;
            }
        }
        return null;
    }


    // for testing
    public void printSummary() {
        logger.info("=== Message Properties Configuration Summary ===");
        logger.info("Total configured instances: {}", instances.size());

        Map<String, ProducerInfo> enabledProducers = getEnabledProducers();
        Map<String, ConsumerInfo> enabledConsumers = getEnabledConsumers();

        logger.info("Enabled producers ({})", enabledProducers.size());
        for (String businessName : enabledProducers.keySet()) {
            logger.info("  - {} -> {}", businessName, instances.get(businessName).getEventHubName());
        }

        logger.info("Enabled consumers ({})", enabledConsumers.size());
        for (String businessName : enabledConsumers.keySet()) {
            EventHubInstance instance = instances.get(businessName);
            logger.info("  - {} -> {} [group: {}]",
                    businessName,
                    instance.getEventHubName(),
                    instance.getConsumerConnector().getConsumerGroup());
        }
        logger.info("================================================");
    }
}