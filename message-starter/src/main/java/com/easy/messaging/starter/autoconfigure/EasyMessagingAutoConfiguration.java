package com.easy.messaging.starter.autoconfigure;

import com.easy.messaging.core.config.EasyMessagingProperties;
import com.easy.messaging.core.consumer.MessageConsumer;
import com.easy.messaging.core.producer.MessageProducer;
import com.easy.messaging.eventhub.EventHubMessageConsumer;
import com.easy.messaging.eventhub.EventHubMessageProducer;
import com.easy.messaging.starter.listener.MessageListenerRegistrar;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(EasyMessagingProperties.class)
@ConditionalOnProperty(prefix="easy.messaging", name="enabled", havingValue="true", matchIfMissing=true)
public class EasyMessagingAutoConfiguration {

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
    public MessageListenerRegistrar messageListenerRegistrar(ApplicationContext context,
                                                             MessageConsumer consumer,
                                                             EasyMessagingProperties props) {
        return new MessageListenerRegistrar(context, consumer, props);
    }
}
