package com.easy.messaging.autoconfig;

import com.azure.messaging.eventhubs.EventProcessorClient;
import com.azure.messaging.eventhubs.EventProcessorClientBuilder;
import com.azure.messaging.eventhubs.models.EventPosition;

import org.springframework.stereotype.Component;

@Component
public class ConsumerFactory {

    public EventProcessorClient createConsumer(MessagingProperties.ConsumerInfo info) {

        MessagingProperties.ConcurrencyConfig concurrency = info.getConcurrencyConfig();

        EventProcessorClientBuilder builder = new EventProcessorClientBuilder()
                .consumerGroup(info.getConsumerGroup())
                .connectionString(info.getConnectionString(), info.getEventHubName())
                .initialPartitionEventPosition(p -> EventPosition.latest());

        if (concurrency != null) {
            builder = builder.prefetchCount(concurrency.getPrefetchCount());
        }

        // 监听器留给用户扩展，这里给出默认空实现
        builder = builder
                .processEvent(event -> {})
                .processError(error -> {});

        return builder.buildEventProcessorClient();
    }
}
