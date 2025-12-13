package com.easy.messaging.autoconfig;

import com.azure.messaging.eventhubs.EventHubClientBuilder;
import com.azure.messaging.eventhubs.EventHubProducerClient;
import org.springframework.stereotype.Component;

@Component
public class ProducerFactory {

    public EventHubProducerClient createProducer(MessagingProperties.ProducerInfo info) {

        MessagingProperties.RetryConfig retry = info.getRetryConfig();

        EventHubClientBuilder builder = new EventHubClientBuilder()
                .connectionString(info.getConnectionString(), info.getEventHubName());

//        if (retry != null) {
//            builder = builder.retryOptions(ro -> {
//                ro.setMaxRetries(retry.getMaxAttempts());
//                ro.setTryTimeout(java.time.Duration.ofMillis(retry.getMaxIntervalMs()));
//            });
//        }

        return builder.buildProducerClient();
    }
}
