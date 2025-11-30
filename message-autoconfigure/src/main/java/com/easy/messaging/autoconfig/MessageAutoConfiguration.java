package com.easy.messaging.autoconfig;

import com.easy.messaging.core.consumer.MessageConsumer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.easy.messaging.core.producer.MessageProducer;

@Configuration
@ConditionalOnClass({MessageProducer.class, MessageConsumer.class})
@EnableConfigurationProperties(EasyMessagingProperties.class)
public class MessageAutoConfiguration {

//    @Bean
//    @ConditionalOnMissingBean
//    public EventHubFactory eventHubFactory() {
//        return new EventHubFactory();
//    }

//    @Bean
//    @ConditionalOnMissingBean
//    @ConditionalOnProperty(prefix = "message.eventhub", name = "enabled", havingValue = "true")
//    public MessageProducer messagePublisher(EventHubFactory factory,
//                                            EasyMessagingProperties properties) {
//        EasyMessagingProperties.EventHub eventHubProps = properties.getEventhub();
//        return factory.createPublisher(
//                eventHubProps.getConnectionString(),
//                eventHubProps.getEventHubName()
//        );
//    }

//    @Bean
//    @ConditionalOnMissingBean
//    public MessageTemplate messageTemplate(MessagePublisher publisher) {
//        return new MessageTemplate(publisher);
//    }
}
