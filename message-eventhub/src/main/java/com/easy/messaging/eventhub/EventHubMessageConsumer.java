package com.easy.messaging.eventhub;

import com.azure.messaging.eventhubs.EventData;
import com.azure.messaging.eventhubs.EventProcessorClient;
import com.azure.messaging.eventhubs.EventProcessorClientBuilder;
import com.azure.messaging.eventhubs.checkpointstore.blob.BlobCheckpointStore;
import com.easy.messaging.core.message.Message;
import com.easy.messaging.core.message.internal.MessageImpl;
import com.easy.messaging.core.consumer.MessageConsumer;
import com.easy.messaging.core.listener.MessageHandler;
import com.easy.messaging.core.consumer.MessageSubscription;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class EventHubMessageConsumer implements MessageConsumer {

    private final EventProcessorClient processorClient;

    public EventHubMessageConsumer(EventProcessorClient processorClient) {
        this.processorClient = processorClient;
        this.processorClient.start();
    }


    @Override
    public MessageSubscription subscribe(String subscriberId, Set<String> channels, MessageHandler handler) {
        //TODO:
        return null;
    }

    @Override
    public String getId() {
        return "";
    }

    @Override
    public void close() {
        //TODO:
    }

    public static EventProcessorClient buildProcessor(String connectionString,
                                                      String eventHubName,
                                                      String consumerGroup,
                                                      BlobCheckpointStore checkpointStore,
                                                      MessageHandler handler) {
        return new EventProcessorClientBuilder()
                .connectionString(connectionString, eventHubName)
                .consumerGroup(consumerGroup)
                .processEvent(partitionEvent -> {
                    EventData eventData = partitionEvent.getEventData();
                    if (eventData != null) {
                        Message msg = toMessage(eventData);
                        try {
                            handler.accept(msg);
                            partitionEvent.updateCheckpoint();
                        } catch (Exception ex) {
                            ex.printStackTrace(); //TODO: update here
                        }
                    }
                })
                .processError(errorContext -> {
                    // 记录错误
                    errorContext.getThrowable().printStackTrace();
                })
                .checkpointStore(checkpointStore) // Must be：BlobCheckpointStore
                .buildEventProcessorClient();
    }

    private static Message toMessage(EventData eventData) {
        Map<String, String> headers = new HashMap<>();
        eventData.getProperties().forEach((k, v) -> headers.put(k, String.valueOf(v)));
        String payload = eventData.getBodyAsString();
        return new MessageImpl(payload, headers);
    }
}
