package com.jaly.messaging.autoconfigure;

import com.jaly.messaging.core.config.EasyMessagingProperties;
import com.jaly.messaging.core.consumer.MessageConsumer;
import com.jaly.messaging.core.producer.MessageProducer;
import com.jaly.messaging.eventhub.EventHubMessageConsumer;
import com.jaly.messaging.eventhub.EventHubMessageProducer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(MessageProducer.class)
@EnableConfigurationProperties(EasyMessagingProperties.class)
public class MessagingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MessageProducer messageProducer(EventHubMessageProducer producer) {
        return producer;
    }

    @Bean
    @ConditionalOnMissingBean
    public MessageConsumer messageConsumer(EventHubMessageConsumer consumer) {
        return consumer;
    }

    @Bean
    public MessageListenerRegistrar messageListenerRegistrar() {
        return new MessageListenerRegistrar();
    }
}
