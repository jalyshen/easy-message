package com.easy.messaging.autoconfig;

import com.azure.messaging.eventhubs.EventHubClientBuilder;
import com.azure.messaging.eventhubs.EventHubProducerClient;
import com.azure.messaging.eventhubs.checkpointstore.blob.BlobCheckpointStore;
import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
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
        MessagingProperties.CheckpointStoreConfig config = props.getCheckpointStore();

        log.info("Prepare to build BlobCheckpointStore");
        if (config == null) {
            log.error("config is null！Spring no configuration！");
        } else {
            log.info("AccountName: {}", config.getAccountName());
            log.info("ContainerName: {}", config.getContainerName());
            log.info("AccountKey length: {}", (config.getAccountKey() == null ? "null" : config.getAccountKey().length()));
        }

        if (config == null ||
                config.getAccountName() == null ||
                config.getAccountKey() == null ||
                config.getContainerName() == null) {

            log.warn("Checkpoint Store not configured");
            log.warn("Please check message.checkpoint-store: account-name, account-key, container-name");
            return null;
        }

        String accountName = config.getAccountName();
        String accountKey = config.getAccountKey();
        String containerName = config.getContainerName();

        // 1. build up Endpoint URL
        String endpoint = String.format("https://%s.blob.core.windows.net", accountName);

        // 2. create credential
        StorageSharedKeyCredential credential = new StorageSharedKeyCredential(accountName, accountKey);

        // 3. build update BlobContainerAsyncClient
        BlobContainerAsyncClient blobContainerAsyncClient = new BlobContainerClientBuilder()
                .endpoint(endpoint)
                .credential(credential)
                .containerName(containerName)
                .buildAsyncClient();

        // 4. initial container
        try {
            Boolean exists = blobContainerAsyncClient.exists().block();
            if (exists != null && !exists) {
                blobContainerAsyncClient.create().block();
                log.info("create Checkpoint container: {}", containerName);
            }
        } catch (Exception e) {
            log.error("initial Checkpoint Store container failed，Please check AccountName/Key", e);
            throw e;
        }

        return new BlobCheckpointStore(blobContainerAsyncClient);
    }


    // 2. Listener Container (Consumer Lifecycle Manager)
    @Bean
    public EventHubListenerContainer eventHubListenerContainer(
            MessagingProperties props,
            ObjectProvider<BlobCheckpointStore> checkpointStore,
            ObjectProvider<List<MessageInterceptor>> interceptorsProvider) {

        EventHubListenerContainer container = new EventHubListenerContainer(checkpointStore.getIfAvailable());

        // register interceptors
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

        // Go though configuration to create Producer Client
        Map<String, MessagingProperties.ProducerInfo> enabledProducers = props.getEnabledProducers();

        enabledProducers.forEach((name, info) -> {
            log.info("Creating producer client for business: {} -> hub: {}", name, info.getEventHubName());

            EventHubProducerClient client = new EventHubClientBuilder()
                    .connectionString(info.getConnectionString(), info.getEventHubName())
                    .buildProducerClient();

            EventHubProducerClientWrapper wrapper = new EventHubProducerClientWrapper(client);

            producerMap.put(name, wrapper);

            // using topic name as key
            if (!producerMap.containsKey(info.getEventHubName())) {
                producerMap.put(info.getEventHubName(), wrapper);
            }
        });

        if (props.getDefaults().getDestinations() != null) {
            props.getDefaults().getDestinations().forEach((logicalName, physicalName) -> {
                MessageProducer targetProducer = producerMap.get(physicalName);

                if (targetProducer != null) {
                    producerMap.put(logicalName, targetProducer);
                    log.info("Mapped logical destination [{}] -> physical producer [{}]", logicalName, physicalName);
                } else {
                    log.warn("Configured logical destination [{}] maps to [{}], but no Producer found for that EventHub.", logicalName, physicalName);
                }
            });
        }

        RoutingMessageProducer routingProducer = new RoutingMessageProducer(producerMap);

        // register interceptors
        List<MessageInterceptor> interceptors = interceptorsProvider.getIfAvailable();
        if (interceptors != null) {
            routingProducer.addInterceptors(interceptors);
        }

        return routingProducer;
    }

}
