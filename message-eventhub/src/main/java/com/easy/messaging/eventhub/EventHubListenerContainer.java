package com.easy.messaging.eventhub;

import com.azure.messaging.eventhubs.EventData;
import com.azure.messaging.eventhubs.EventProcessorClient;
import com.azure.messaging.eventhubs.EventProcessorClientBuilder;
import com.azure.messaging.eventhubs.checkpointstore.blob.BlobCheckpointStore;
import com.azure.messaging.eventhubs.models.ErrorContext;
import com.azure.messaging.eventhubs.models.EventContext;
import com.azure.messaging.eventhubs.models.EventPosition;
import com.easy.messaging.core.listener.MessageHandler;
import com.easy.messaging.core.message.Message;
import com.easy.messaging.core.message.MessageInterceptor;
import com.easy.messaging.core.message.internal.MessageImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EventHubListenerContainer implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(EventHubListenerContainer.class);
    private boolean isRunning = false;

    private final List<EventProcessorClient> processors = new ArrayList<>();

    private final List<MessageInterceptor> interceptors = new ArrayList<>();

    // dependence BlobCheckpointStore (must be injected from  AutoConfig)
     private final BlobCheckpointStore checkpointStore;

    public EventHubListenerContainer(BlobCheckpointStore checkpointStore) {
        this.checkpointStore = checkpointStore;
    }

    public void addInterceptor(MessageInterceptor interceptor) {
        this.interceptors.add(interceptor);
    }
    public void addInterceptors(List<MessageInterceptor> interceptors) {
        this.interceptors.addAll(interceptors);
    }

    /**
     * create Client instance
     */
    public void registerListener(String connectionString,
                                 String eventHubName,
                                 String consumerGroup,
                                 MessageHandler handler) {

        if (this.checkpointStore == null) {
            log.error("CheckpointStore is NULL. Could NOT register listener");
            log.error("Please check: 1. MessagingProperties should include 'CheckpointStoreConfig'");
            log.error("              2. application.yml should include message.checkpoint-store.account-name ");
            return;
        }

        log.info("Registering listener for EventHub: {} Group: {}", eventHubName, consumerGroup);
        EventProcessorClient client = new EventProcessorClientBuilder()
                .connectionString(connectionString, eventHubName)
                .consumerGroup(consumerGroup)
                .checkpointStore(checkpointStore)
                .processEvent(eventContext -> processEvent(eventContext, handler, consumerGroup))
                .processError(this::processError)
                // default, system consumers messages from the earliest
                .initialPartitionEventPosition(map -> EventPosition.earliest()) //EventPosition.latest())
                .buildEventProcessorClient();
        processors.add(client);
    }
    private void processEvent(EventContext eventContext, MessageHandler handler, String subscriberId) {
        EventData eventData = eventContext.getEventData();
        if (eventData == null) return;

        Message message = toMessage(eventData);

        // 1. Interceptor PreReceive
        interceptors.forEach(i -> safeExecute(() -> i.preReceive(message)));

        try {
            // 2. Interceptor PreHandle
            interceptors.forEach(i -> safeExecute(() -> i.preHandle(subscriberId, message)));

            // 3.
            handler.accept(message);

            // 4. Checkpoint
            eventContext.updateCheckpoint();

            // 5. Interceptor PostHandle (Success)
            interceptors.forEach(i -> safeExecute(() -> i.postHandle(subscriberId, message, null)));

        } catch (Exception e) {
            log.error("Error processing message", e);
            // 5. Interceptor PostHandle (Error)
            interceptors.forEach(i -> safeExecute(() -> i.postHandle(subscriberId, message, e)));
        } finally {
            // 6. Interceptor PostReceive
            interceptors.forEach(i -> safeExecute(() -> i.postReceive(message)));
        }
    }

    private void processError(ErrorContext errorContext) {
        log.error("Error occurred in partition processor for partition {}",
                errorContext.getPartitionContext().getPartitionId(),
                errorContext.getThrowable());
    }
    private Message toMessage(EventData eventData) {
        Map<String, String> headers = new java.util.HashMap<>();
        eventData.getProperties().forEach((k, v) -> headers.put(k, String.valueOf(v)));

        // 补充 System Properties
        if (eventData.getEnqueuedTime() != null) {
            headers.put(Message.DATE, eventData.getEnqueuedTime().toString());
        }

        String payload = eventData.getBodyAsString();
        return new MessageImpl(payload, headers);
    }

    private void safeExecute(Runnable r) {
        try { r.run(); } catch (Exception e) { log.warn("Interceptor error", e); }
    }
    @Override
    public void start() {
        if (isRunning) return;
        log.info("Starting EventHubListenerContainer with {} processors...", processors.size());
        processors.forEach(EventProcessorClient::start);
        isRunning = true;
    }
    @Override
    public void stop() {
        if (!isRunning) return;
        log.info("Stopping EventHubListenerContainer...");
        processors.forEach(EventProcessorClient::stop);
        isRunning = false;
    }
    @Override
    public boolean isRunning() {
        return isRunning;
    }
    // 保证在大多数 Bean 初始化后启动
    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }
}
