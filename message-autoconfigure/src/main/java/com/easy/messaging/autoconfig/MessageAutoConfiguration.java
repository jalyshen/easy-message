package com.easy.messaging.autoconfig;

import com.azure.messaging.eventhubs.EventHubClientBuilder;
import com.azure.messaging.eventhubs.EventHubProducerClient;
import com.azure.messaging.eventhubs.checkpointstore.blob.BlobCheckpointStore;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.easy.messaging.core.message.MessageInterceptor;
import com.easy.messaging.core.producer.MessageProducer;
import com.easy.messaging.core.producer.RoutingMessageProducer;
import com.easy.messaging.eventhub.EventHubListenerContainer;
import com.easy.messaging.eventhub.EventHubProducerClientWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(MessagingProperties.class)
@Import(TracingConfiguration.class)
public class MessageAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(MessageAutoConfiguration.class);

    // 1. Checkpoint Store (Azure Blob)
    @Bean
    @ConditionalOnMissingBean
    public BlobCheckpointStore blobCheckpointStore(MessagingProperties props) {
        String conn = props.getCheckpointConnectionString();
        String container = props.getCheckpointContainerName();

        if (conn == null || container == null) {
            log.warn("Checkpoint storage not configured. Consumers might fail.");
            return null;
        }

        // 1. 构建异步客户端 (AsyncClient)
        com.azure.storage.blob.BlobContainerAsyncClient blobContainerAsyncClient = new BlobContainerClientBuilder()
                .connectionString(conn)
                .containerName(container)
                .buildAsyncClient();

        // 2. 初始化容器：在启动时阻塞检查是否存在，不存在则创建
        // block() 是安全的，因为这只在应用启动时的这个 Bean 初始化阶段执行一次
        Boolean exists = blobContainerAsyncClient.exists().block();
        if (exists != null && !exists) {
            blobContainerAsyncClient.create().block();
        }

        // 3. 传入异步客户端
        return new BlobCheckpointStore(blobContainerAsyncClient);
    }

    // 2. Listener Container (Consumer Lifecycle Manager)
    @Bean
    public EventHubListenerContainer eventHubListenerContainer(
            MessagingProperties props,
            ObjectProvider<BlobCheckpointStore> checkpointStore,
            ObjectProvider<List<MessageInterceptor>> interceptorsProvider) {

        EventHubListenerContainer container = new EventHubListenerContainer(checkpointStore.getIfAvailable());

        // 注入拦截器
        List<MessageInterceptor> interceptors = interceptorsProvider.getIfAvailable();
        if (interceptors != null) {
            container.addInterceptors(interceptors);
        }

        return container;
    }

    // 3. Listener Scanner (Scans beans & registers to Container)
    @Bean
    public ListenerScanner listenerScanner(EventHubListenerContainer container, MessagingProperties props) {
        return new ListenerScanner(container, props);
    }

    // 4. Producer (Routing)
    @Bean
    @ConditionalOnMissingBean(MessageProducer.class)
    public MessageProducer messageProducer(MessagingProperties props,
                                           ObjectProvider<List<MessageInterceptor>> interceptorsProvider) {

        Map<String, MessageProducer> producerMap = new HashMap<>();

        // 遍历配置，创建底层 Producer Client
        Map<String, MessagingProperties.ProducerInfo> enabledProducers = props.getEnabledProducers();

        enabledProducers.forEach((name, info) -> {
            log.info("Creating producer client for business: {} -> hub: {}", name, info.getEventHubName());

            EventHubProducerClient client = new EventHubClientBuilder()
                    .connectionString(info.getConnectionString(), info.getEventHubName())
                    .buildProducerClient();

            producerMap.put(name, new EventHubProducerClientWrapper(client));

            // 同时把 topic 名字也注册进去，方便直接用 topic 发送
            if (!producerMap.containsKey(info.getEventHubName())) {
                producerMap.put(info.getEventHubName(), new EventHubProducerClientWrapper(client));
            }
        });

        RoutingMessageProducer routingProducer = new RoutingMessageProducer(producerMap);

        // 注入拦截器
        List<MessageInterceptor> interceptors = interceptorsProvider.getIfAvailable();
        if (interceptors != null) {
            routingProducer.addInterceptors(interceptors);
        }

        return routingProducer;
    }

}
