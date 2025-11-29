package com.jaly.messaging.autoconfigure;

import com.jaly.messaging.core.MessageConsumer;
import com.jaly.messaging.core.MessageProducer;
import com.jaly.messaging.eventhub.EventHubMessageConsumer;
import com.jaly.messaging.eventhub.EventHubMessageProducer;

@Configuration
@ConditionalOnClass(MessageProducer.class)
@EnableConfigurationProperties(EmspMessagingProperties.class)
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
